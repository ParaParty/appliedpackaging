package com.warmthdawn.appliedpackaging.core.sequence_buffer;

import com.warmthdawn.appliedpackaging.config.APServerConfig;
import com.warmthdawn.appliedpackaging.registry.APBlocks;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferBlock;
import com.warmthdawn.appliedpackaging.world.block.SequenceBufferVisualState;
import com.warmthdawn.appliedpackaging.world.block.entity.SequenceBufferBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SequenceBufferTopology {
    private static final int UPDATE_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS;

    private SequenceBufferTopology() {
    }

    public static void schedule(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).is(APBlocks.SEQUENCE_BUFFER.get())) {
            level.scheduleTick(pos, APBlocks.SEQUENCE_BUFFER.get(), 1);
        }
    }

    public static void scheduleAround(ServerLevel level, BlockPos pos) {
        schedule(level, pos);
        for (Direction direction : Direction.values()) {
            schedule(level, pos.relative(direction));
        }
    }

    public static void reconcile(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(APBlocks.SEQUENCE_BUFFER.get())) {
            return;
        }
        SequenceBufferVisualState visual = state.getValue(SequenceBufferBlock.STATE);
        if (visual.isDirected() && !state.getValue(SequenceBufferBlock.DIRECTIONAL)) {
            state = state.setValue(SequenceBufferBlock.DIRECTIONAL, true);
            level.setBlock(pos, state, UPDATE_FLAGS);
        }
        if (visual == SequenceBufferVisualState.UNFORMED_DIRECTED) {
            tryForm(level, pos, state.getValue(SequenceBufferBlock.FACING));
            return;
        }
        if (visual == SequenceBufferVisualState.ENDPOINT) {
            rebuildEndpoint(level, pos);
            return;
        }
        if (visual.isMember()) {
            SequenceBufferBlockEntity member = blockEntity(level, pos).orElse(null);
            if (member == null) {
                return;
            }
            SequenceBufferBlockEntity endpoint = resolveEndpoint(member).orElse(null);
            if (endpoint == null || !members(endpoint).contains(member)) {
                detachTail(level, member);
            } else {
                schedule(level, endpoint.getBlockPos());
            }
        }
    }

    public static boolean tryForm(ServerLevel level, BlockPos endpointPos, Direction direction) {
        BlockState endpointState = level.getBlockState(endpointPos);
        if (!endpointState.is(APBlocks.SEQUENCE_BUFFER.get())
                || endpointState.getValue(SequenceBufferBlock.STATE)
                        != SequenceBufferVisualState.UNFORMED_DIRECTED) {
            return false;
        }

        int limit = APServerConfig.maxSequenceBufferLength();
        List<BlockPos> positions = new ArrayList<>();
        positions.add(endpointPos.immutable());
        for (int offset = 1; offset < limit; offset++) {
            BlockPos candidate = endpointPos.relative(direction, offset);
            if (!level.hasChunkAt(candidate)) {
                return false;
            }
            BlockState state = level.getBlockState(candidate);
            if (!state.is(APBlocks.SEQUENCE_BUFFER.get())) {
                break;
            }
            if (state.getValue(SequenceBufferBlock.STATE).isFormed()) {
                return false;
            }
            positions.add(candidate.immutable());
        }
        BlockPos beyondLimit = endpointPos.relative(direction, limit);
        if (level.hasChunkAt(beyondLimit)
                && level.getBlockState(beyondLimit).is(APBlocks.SEQUENCE_BUFFER.get())) {
            return false;
        }
        if (positions.size() < 2) {
            return false;
        }
        return commitStructure(level, positions, direction);
    }

    public static void rebuildEndpoint(ServerLevel level, BlockPos endpointPos) {
        SequenceBufferBlockEntity endpoint = blockEntity(level, endpointPos).orElse(null);
        BlockState endpointState = level.getBlockState(endpointPos);
        if (endpoint == null
                || !endpointState.is(APBlocks.SEQUENCE_BUFFER.get())
                || endpointState.getValue(SequenceBufferBlock.STATE) != SequenceBufferVisualState.ENDPOINT) {
            return;
        }
        Direction direction = endpointState.getValue(SequenceBufferBlock.SEQUENCE_DIRECTION);
        int limit = APServerConfig.maxSequenceBufferLength();
        List<BlockPos> positions = new ArrayList<>();
        positions.add(endpointPos.immutable());
        for (int offset = 1; offset < limit; offset++) {
            BlockPos candidate = endpointPos.relative(direction, offset);
            if (!level.hasChunkAt(candidate)) {
                break;
            }
            BlockState state = level.getBlockState(candidate);
            if (!state.is(APBlocks.SEQUENCE_BUFFER.get())) {
                break;
            }
            SequenceBufferVisualState visual = state.getValue(SequenceBufferBlock.STATE);
            SequenceBufferBlockEntity candidateEntity = blockEntity(level, candidate).orElse(null);
            if (candidateEntity == null || visual == SequenceBufferVisualState.ENDPOINT) {
                break;
            }
            if (visual.isFormed() && !candidateEntity.controllerPos().equals(endpointPos)) {
                break;
            }
            positions.add(candidate.immutable());
        }
        if (positions.size() < 2) {
            disassembleEndpoint(level, endpointPos);
            return;
        }
        commitStructure(level, positions, direction);
    }

    private static boolean commitStructure(ServerLevel level, List<BlockPos> positions, Direction direction) {
        SequenceBufferBlockEntity endpoint = blockEntity(level, positions.get(0)).orElse(null);
        if (endpoint == null || !endpoint.canBecomeEndpoint()) {
            return false;
        }
        SequenceBufferConfiguration authoritativeConfig = endpoint.configurationCopy();
        for (int index = 0; index < positions.size(); index++) {
            BlockPos pos = positions.get(index);
            SequenceBufferBlockEntity blockEntity = blockEntity(level, pos).orElse(null);
            if (blockEntity == null) {
                return false;
            }
            BlockState oldState = level.getBlockState(pos);
            BlockState newState;
            if (index == 0) {
                newState = oldState
                        .setValue(SequenceBufferBlock.STATE, SequenceBufferVisualState.ENDPOINT)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, true)
                        .setValue(SequenceBufferBlock.SEQUENCE_DIRECTION, direction)
                        .setValue(SequenceBufferBlock.AXIS, direction.getAxis())
                        .setValue(SequenceBufferBlock.TAIL, false);
            } else {
                Direction oldFacing = oldState.getValue(SequenceBufferBlock.FACING);
                boolean keepOutputDirection = SequenceBufferBlock.hasOwnDirection(oldState)
                        && oldFacing.getAxis() != direction.getAxis();
                newState = oldState
                        .setValue(
                                SequenceBufferBlock.STATE,
                                keepOutputDirection
                                        ? SequenceBufferVisualState.MEMBER_DIRECTED
                                        : SequenceBufferVisualState.MEMBER)
                        .setValue(SequenceBufferBlock.DIRECTIONAL, SequenceBufferBlock.hasOwnDirection(oldState))
                        .setValue(SequenceBufferBlock.SEQUENCE_DIRECTION, direction)
                        .setValue(SequenceBufferBlock.AXIS, direction.getAxis())
                        .setValue(SequenceBufferBlock.TAIL, index == positions.size() - 1);
            }
            if (oldState != newState) {
                level.setBlock(pos, newState, UPDATE_FLAGS);
            }
            blockEntity.assignTopology(positions.get(0), direction, index);
            blockEntity.applyControllerConfiguration(authoritativeConfig);
        }
        return true;
    }

    public static void disassembleEndpoint(ServerLevel level, BlockPos endpointPos) {
        SequenceBufferBlockEntity endpoint = blockEntity(level, endpointPos).orElse(null);
        if (endpoint == null) {
            return;
        }
        List<SequenceBufferBlockEntity> currentMembers = structureBlocks(endpoint);
        for (SequenceBufferBlockEntity member : currentMembers) {
            detachOne(level, member);
        }
    }

    public static void onBlockRemoved(ServerLevel level, BlockPos removedPos, BlockState oldState) {
        SequenceBufferVisualState visual = oldState.getValue(SequenceBufferBlock.STATE);
        BlockEntity raw = level.getBlockEntity(removedPos);
        if (!(raw instanceof SequenceBufferBlockEntity removed)) {
            return;
        }
        if (visual == SequenceBufferVisualState.ENDPOINT) {
            Direction direction = oldState.getValue(SequenceBufferBlock.SEQUENCE_DIRECTION);
            int limit = APServerConfig.maxSequenceBufferLength();
            for (int offset = 1; offset < limit; offset++) {
                BlockPos candidate = removedPos.relative(direction, offset);
                SequenceBufferBlockEntity member = blockEntity(level, candidate).orElse(null);
                if (member == null || !member.controllerPos().equals(removedPos)) {
                    break;
                }
                detachOne(level, member);
            }
        } else if (visual.isMember()) {
            schedule(level, removed.controllerPos());
            schedule(level, removedPos.relative(removed.sequenceDirection()));
        }
    }

    private static void detachTail(ServerLevel level, SequenceBufferBlockEntity first) {
        BlockPos oldController = first.controllerPos();
        Direction direction = first.sequenceDirection();
        BlockPos cursor = first.getBlockPos();
        int limit = APServerConfig.maxSequenceBufferLength();
        for (int i = 0; i < limit; i++) {
            SequenceBufferBlockEntity member = blockEntity(level, cursor).orElse(null);
            if (member == null || !member.controllerPos().equals(oldController)) {
                break;
            }
            detachOne(level, member);
            cursor = cursor.relative(direction);
        }
    }

    private static void detachOne(ServerLevel level, SequenceBufferBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(APBlocks.SEQUENCE_BUFFER.get())) {
            return;
        }
        SequenceBufferVisualState visual = state.getValue(SequenceBufferBlock.STATE);
        SequenceBufferVisualState detachedState = SequenceBufferBlock.hasOwnDirection(state)
                ? SequenceBufferVisualState.UNFORMED_DIRECTED
                : SequenceBufferVisualState.UNFORMED;
        blockEntity.clearTopology();
        level.setBlock(
                pos,
                state.setValue(SequenceBufferBlock.STATE, detachedState)
                        .setValue(SequenceBufferBlock.TAIL, false),
                UPDATE_FLAGS);
    }

    public static Optional<SequenceBufferBlockEntity> resolveEndpoint(SequenceBufferBlockEntity blockEntity) {
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        BlockState state = blockEntity.getBlockState();
        if (state.is(APBlocks.SEQUENCE_BUFFER.get())
                && state.getValue(SequenceBufferBlock.STATE) == SequenceBufferVisualState.ENDPOINT) {
            return Optional.of(blockEntity);
        }
        SequenceBufferBlockEntity endpoint = blockEntity(level, blockEntity.controllerPos()).orElse(null);
        if (endpoint == null) {
            return Optional.empty();
        }
        BlockState endpointState = endpoint.getBlockState();
        if (!endpointState.is(APBlocks.SEQUENCE_BUFFER.get())
                || endpointState.getValue(SequenceBufferBlock.STATE) != SequenceBufferVisualState.ENDPOINT) {
            return Optional.empty();
        }
        return Optional.of(endpoint);
    }

    public static List<SequenceBufferBlockEntity> members(SequenceBufferBlockEntity anyMember) {
        SequenceBufferBlockEntity endpoint = resolveEndpoint(anyMember).orElse(null);
        if (endpoint == null || !(endpoint.getLevel() instanceof ServerLevel level)) {
            return List.of(anyMember);
        }
        BlockState endpointState = endpoint.getBlockState();
        Direction direction = endpointState.getValue(SequenceBufferBlock.SEQUENCE_DIRECTION);
        List<SequenceBufferBlockEntity> result = new ArrayList<>();
        int limit = APServerConfig.maxSequenceBufferLength();
        for (int index = 1; index < limit; index++) {
            BlockPos pos = endpoint.getBlockPos().relative(direction, index);
            SequenceBufferBlockEntity current = blockEntity(level, pos).orElse(null);
            if (current == null || !current.controllerPos().equals(endpoint.getBlockPos())) {
                break;
            }
            BlockState state = current.getBlockState();
            if (!state.is(APBlocks.SEQUENCE_BUFFER.get())
                    || !state.getValue(SequenceBufferBlock.STATE).isFormed()) {
                break;
            }
            result.add(current);
        }
        return List.copyOf(result);
    }

    private static List<SequenceBufferBlockEntity> structureBlocks(SequenceBufferBlockEntity endpoint) {
        List<SequenceBufferBlockEntity> result = new ArrayList<>();
        result.add(endpoint);
        result.addAll(members(endpoint));
        return List.copyOf(result);
    }

    private static Optional<SequenceBufferBlockEntity> blockEntity(ServerLevel level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof SequenceBufferBlockEntity sequenceBuffer
                ? Optional.of(sequenceBuffer)
                : Optional.empty();
    }
}
