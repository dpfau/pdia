package edu.columbia.stat.wood.hpyp;

import java.io.Serializable;
import java.util.Arrays;
import java.util.TreeMap;

public class BaseRestaurant extends Restaurant implements Serializable {

    public int alphabetSize; // If -1, this is a geometric distribution, if positive, uniform distribution
    public int customers;
    public double lambda;
    private static final long serialVersionUID = 1L;

    public BaseRestaurant(int alphabetSize) {
        this.alphabetSize = alphabetSize;
        customers = 0;
    }

    public BaseRestaurant() {
        customers = 0;
        alphabetSize = -1;
        lambda = 0.001;
    }

    public void set(int cust) {
        customers = cust;
    }

    @Override
    public double predictiveProbability(int type) {
        if (alphabetSize > -1) {
            return 1.0 / alphabetSize;
        }
        return Math.exp(-1.0 * type * lambda) - Math.exp(-1.0 * (type + 1) * lambda);
    }

    @Override
    public int[] seat(int type) {
        customers += 1;
        return null;
    }

    @Override
    public void seatKN(int type) {
        customers += 1;
    }

    @Override
    public void unseat(int type) {
        if (alphabetSize > -1) {
            customers -= 1;
        }
        assert (customers > -1);
    }

    @Override
    public double logLik() {
        if (alphabetSize > -1) {
            return customers * Math.log(1.0 / alphabetSize);
        }
        return 0.0;
    }

    @Override
    public void predictiveProbability(double[] p) {
        if (alphabetSize > -1) {
            assert (p.length == alphabetSize);
            Arrays.fill(p, 1.0D / alphabetSize);
        } else {
            throw new RuntimeException("not supported in the case of infinite base distribution");
        }
    }

    @Override
    public int generate() {
        double r = RNG.nextDouble();

        int ind = -1;
        double cuSum = 0.0;
        while (cuSum <= r) {
            ind++;
            if (alphabetSize > -1) {
                cuSum += 1.0 / alphabetSize;
            } else {
                cuSum += Math.exp(-1.0 * ind * lambda) - Math.exp(-1.0D * (ind + 1) * lambda);
            }
        }

        return ind;
    }

    public int generateNewType(TreeMap<Integer, int[]> tblMap) {
        assert (this.alphabetSize == -1);

        double totalWeight = 1.0;
        for (Integer type : tblMap.keySet()) {
            totalWeight -= Math.exp(-1.0 * type * lambda) - Math.exp(-1.0D * (type + 1) * lambda);
        }

        double r = RNG.nextDouble();
        int newType = -1;
        double cuSum = 0.0D;
        while (cuSum <= r) {
            newType++;
            if (tblMap.get(newType) == null) {
                double pp = predictiveProbability(newType);
                cuSum += pp / totalWeight;
            }
        }

        return newType;
    }
}
