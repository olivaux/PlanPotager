package eu.planpotager.PlanPotager.garden.domain;

public enum PlantState {
    A_PLANTER,
    PLANTEE,
    A_RECOLTER,
    RECOLTEE;

    public boolean canTransitionTo(PlantState next) {
        return next.ordinal() == this.ordinal() + 1;
    }
}
