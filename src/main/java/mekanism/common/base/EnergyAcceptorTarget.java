package mekanism.common.base;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.EnumFacing;

public class EnergyAcceptorTarget {

    private Map<EnumFacing, EnergyAcceptorWrapper> wrappers = new HashMap<>();
    private Map<EnumFacing, Double> needed = new HashMap<>();
    private Map<EnumFacing, Double> given = new HashMap<>();

    public boolean hasAcceptors() {
        return !wrappers.isEmpty();
    }

    public void addSide(EnumFacing side, EnergyAcceptorWrapper acceptor) {
        wrappers.put(side, acceptor);
    }

    public Map<EnumFacing, EnergyAcceptorWrapper> getWrappers() {
        return wrappers;
    }

    public void addAmount(EnumFacing side, double amountNeeded, boolean canGive) {
        if (canGive) {
            given.put(side, amountNeeded);
        } else {
            needed.put(side, amountNeeded);
        }
    }

    public double sendGivenWithDefault(double amountPer) {
        double sent = 0;
        for (Entry<EnumFacing, Double> giveInfo : given.entrySet()) {
            sent += acceptAmount(giveInfo.getKey(), giveInfo.getValue());
        }
        //If needed is not empty then we default it to the given calculated fair split amount of remaining energy
        for (EnumFacing side : needed.keySet()) {
            sent += acceptAmount(side, amountPer);
        }
        return sent;
    }

    private double acceptAmount(EnumFacing side, double amount) {
        //TODO: Do we need to verify there is an acceptor for that side?
        // We should be able to safely assume that there is given how we get to this point
        EnergyAcceptorWrapper acceptor = wrappers.get(side);
        //Give it power and add how much actually got accepted instead of how much
        // we attempted to give it
        return acceptor.acceptEnergy(side, amount, false);
    }

    public void addGiven(EnumFacing side, double amountNeeded) {
        given.put(side, amountNeeded);
    }

    public Iterator<Entry<EnumFacing, Double>> getNeededIterator() {
        return needed.entrySet().iterator();
    }

    public boolean noneNeeded() {
        return needed.isEmpty();
    }
}