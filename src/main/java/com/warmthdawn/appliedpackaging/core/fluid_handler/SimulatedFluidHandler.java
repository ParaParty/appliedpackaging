package com.warmthdawn.appliedpackaging.core.fluid_handler;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

final class SimulatedFluidHandler implements IFluidHandler {
    private final IFluidHandler source;
    private final FluidStack[] fluids;
    private final int[] capacities;

    private SimulatedFluidHandler(IFluidHandler source) {
        this.source = source;
        this.fluids = new FluidStack[source.getTanks()];
        this.capacities = new int[source.getTanks()];
        for (int tank = 0; tank < source.getTanks(); tank++) {
            this.fluids[tank] = source.getFluidInTank(tank).copy();
            this.capacities[tank] = source.getTankCapacity(tank);
        }
    }

    static SimulatedFluidHandler copyOf(IFluidHandler source) {
        return new SimulatedFluidHandler(source);
    }

    @Override
    public int getTanks() {
        return fluids.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return fluids[tank].copy();
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacities[tank];
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return source.isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        int filled = 0;
        for (int tank = 0; tank < fluids.length && filled < resource.getAmount(); tank++) {
            if (fluids[tank].isEmpty() || !sameFluid(fluids[tank], resource)) {
                continue;
            }
            filled += fillTank(tank, resource, resource.getAmount() - filled, action);
        }
        for (int tank = 0; tank < fluids.length && filled < resource.getAmount(); tank++) {
            if (!fluids[tank].isEmpty()) {
                continue;
            }
            filled += fillTank(tank, resource, resource.getAmount() - filled, action);
        }
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = 0;
        for (int tank = 0; tank < fluids.length && drained < resource.getAmount(); tank++) {
            FluidStack fluid = fluids[tank];
            if (fluid.isEmpty() || !sameFluid(fluid, resource)) {
                continue;
            }
            int amount = Math.min(resource.getAmount() - drained, fluid.getAmount());
            if (action.execute()) {
                fluid.shrink(amount);
                if (fluid.getAmount() <= 0) {
                    fluids[tank] = FluidStack.EMPTY;
                }
            }
            drained += amount;
        }
        return drained <= 0 ? FluidStack.EMPTY : new FluidStack(resource, drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < fluids.length; tank++) {
            FluidStack fluid = fluids[tank];
            if (fluid.isEmpty()) {
                continue;
            }
            int amount = Math.min(maxDrain, fluid.getAmount());
            FluidStack drained = new FluidStack(fluid, amount);
            if (action.execute()) {
                fluid.shrink(amount);
                if (fluid.getAmount() <= 0) {
                    fluids[tank] = FluidStack.EMPTY;
                }
            }
            return drained;
        }
        return FluidStack.EMPTY;
    }

    private int fillTank(int tank, FluidStack resource, int amount, FluidAction action) {
        if (amount <= 0 || !isFluidValid(tank, resource)) {
            return 0;
        }
        int fillAmount = Math.min(amount, capacities[tank] - fluids[tank].getAmount());
        if (fillAmount <= 0) {
            return 0;
        }
        if (action.execute()) {
            if (fluids[tank].isEmpty()) {
                fluids[tank] = new FluidStack(resource, fillAmount);
            } else {
                fluids[tank].grow(fillAmount);
            }
        }
        return fillAmount;
    }

    private static boolean sameFluid(FluidStack first, FluidStack second) {
        return first.isFluidEqual(second) && FluidStack.areFluidStackTagsEqual(first, second);
    }
}
