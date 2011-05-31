package edu.columbia.stat.wood.hpyp;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

public class RestaurantFranchise implements Serializable {

    public Restaurant root;
    public Discounts discounts;
    public Concentrations concentrations;
    private int depth;
    private int alphabetSize;
    private int[] currentContext;
    private static final long serialVersionUID = 1L;
    private double[] dLogLik;
    private double[] cLogLik;
    private int num = 0;

    public RestaurantFranchise(int depth, int alphabetSize) {
        this.alphabetSize = alphabetSize;
        this.depth = depth;

        discounts = new Discounts(new double[]{0.0});
        concentrations = new Concentrations(new double[]{10.0, 10.0});

        root = new Restaurant(new BaseRestaurant(alphabetSize), discounts.get(0), concentrations.get(0));
    }

    public RestaurantFranchise(int depth) {
        this.depth = depth;

        discounts = new Discounts(new double[]{0.5, 0.5});
        concentrations = new Concentrations(new double[]{20.0, 15.0});

        root = new Restaurant(new BaseRestaurant(), discounts.get(0), concentrations.get(0));
        alphabetSize = -1;
    }

    public double continueSequence(int type) {
        double p = seat(type, currentContext);
        updateContext(type);
        return Math.log(p);
    }

    public int depth() {
        return depth;
    }

    public int alphabetSize() {
        return alphabetSize;
    }

    public double seat(int type, int[] context) {
        Restaurant r = get(context);
        double p = r.predictiveProbability(type);
        r.seat(type);

        return p;
    }

    public int generate(int[] context) {
        return get(context).generate();
    }

    public void unseat(int type, int[] context) {
        get(context).unseat(type);
    }

    public double predictiveProbability(int type, int[] context) {
        return getDontAdd(context).predictiveProbability(type);
    }

    public HashMap<Integer, MutableDouble> predictiveProbabilityExistingTypes(int[] context) {
    	HashMap<Integer, MutableDouble> returnMap = new HashMap<Integer, MutableDouble>(root.tableMap.size() + 1);
        Restaurant r = getDontAdd(context);
        double cuSum = 0.0;

        for (Integer type : root.tableMap.keySet()) {
            double p = r.predictiveProbability(type);
            cuSum += p;
            returnMap.put(type, new MutableDouble(p));
        }

        returnMap.put(-1, new MutableDouble(1.0 - cuSum));

        return returnMap;
    }

    public int generateNewType() {
        BaseRestaurant r = (BaseRestaurant) root.getParent();
        return r.generateNewType(root.tableMap);
    }

    public double[] predictiveProbability(int[] context) {
        double[] p = new double[alphabetSize];
        get(context).predictiveProbability(p);
        return p;
    }

    public void sample() {
        sampleSeating(root);
        sampleDiscounts(0.07);
        sampleConcentrations(1.5);
    }

    private void sampleSeating(Restaurant r) {
        for (Restaurant child : r.values()) {
            sampleSeating(child);
        }


        HashSet<Integer> toRemove = new HashSet<Integer>();

        for (Integer key : r.keySet()) {
            Restaurant c = r.get(key);
            if ((c.isEmpty()) && (c.tables() == 0)) {
                toRemove.add(key);
            }
        }

        for (Integer key : toRemove) {
            r.remove(key);
        }

        r.sampleSeatingArrangements();
    }

    public void sampleDiscounts(double proposalSTD) {
        double[] currentDiscounts = new double[discounts.length()];

        logLik();

        double[] currentLogLik = new double[discounts.length()];
        System.arraycopy(dLogLik, 0, currentLogLik, 0, discounts.length());

        for (int i = 0; i < discounts.length(); i++) {
            MutableDouble d = discounts.get(i);
            currentDiscounts[i] = d.doubleVal();
            double proposal = currentDiscounts[i] + Restaurant.RNG.nextGaussian() * proposalSTD;
            proposal = (proposal >= 1.0) || (proposal <= 0.0) ? currentDiscounts[i] : proposal;
            d.set(proposal);
        }

        logLik();

        for (int i = 0; i < discounts.length(); i++) {
            if (Restaurant.RNG.nextDouble() >= Math.exp(dLogLik[i] - currentLogLik[i])) {
                discounts.get(i).set(currentDiscounts[i]);
            }
        }
    }

    public void sampleConcentrations(double proposalSTD) {
        double[] currentConcentrations = new double[concentrations.length()];

        logLik();

        double[] currentLogLik = new double[concentrations.length()];
        System.arraycopy(cLogLik, 0, currentLogLik, 0, concentrations.length());

        for (int i = 0; i < concentrations.length(); i++) {
            MutableDouble c = concentrations.get(i);
            currentConcentrations[i] = c.doubleVal();
            double proposal = currentConcentrations[i] + Restaurant.RNG.nextGaussian() * proposalSTD;
            proposal = proposal <= 0.0 ? currentConcentrations[i] : proposal;
            c.set(proposal);
        }

        logLik();

        for (int i = 0; i < concentrations.length(); i++) {
            double r = Math.exp(cLogLik[i] - concentrations.get(i).doubleVal() - currentLogLik[i] + currentConcentrations[i]);
            if (Restaurant.RNG.nextDouble() >= r) {
                concentrations.get(i).set(currentConcentrations[i]);
            }
        }
    }

    public double logLik() {
        double logLik = root.getParent().logLik();
        dLogLik = new double[discounts.length()];
        cLogLik = new double[concentrations.length()];

        logLik(root, 0);
        for (double d : dLogLik) {
            logLik += d;
        }

        return logLik;
    }

    public void logLik(Restaurant r, int d) {
        int dIndex = d < discounts.length()      ? d : discounts.length()      - 1;
        int cIndex = d < concentrations.length() ? d : concentrations.length() - 1;

        double logLik = r.logLik();

        dLogLik[dIndex] += logLik;
        cLogLik[cIndex] += logLik;

        for (Restaurant child : r.values()) {
            logLik(child, d + 1);
        }
    }

    public Restaurant getDontAdd(int[] context) {
        if ((context == null) || (context.length == 0)) {
            return root;
        }

        int ci = context.length - 1;
        int d = 0;
        Restaurant current = root;
        Restaurant child = null;

        while ((d < depth) && (ci > -1)) {
            child = current.get(context[ci]);
            if (child == null) {
                break;
            }
            current = child;
            ci--;
            d++;
        }

        return current;
    }

    public Restaurant get(int[] context) {
        if ((context == null) || (context.length == 0)) {
            return root;
        }

        int ci = context.length - 1;
        int d = 0;
        Restaurant current = root;
        Restaurant child = null;

        while ((d < depth) && (ci > -1)) {
            child = current.get(context[ci]);
            if (child == null) {
                child = new Restaurant(current, discounts.get(d + 1), concentrations.get(d + 1));
                current.put(context[ci], child);
            }
            current = child;
            ci--;
            d++;
        }

        return current;
    }

    private void updateContext(int obs) {
        if (depth != 0) {
            if (currentContext == null) {
                currentContext = new int[]{obs};
            } else if (currentContext.length < depth) {
                int[] newContext = new int[currentContext.length + 1];
                System.arraycopy(currentContext, 0, newContext, 0, currentContext.length);
                newContext[currentContext.length] = obs;
                currentContext = newContext;
            } else {
                for (int i = 0; i < depth - 1; i++) {
                    currentContext[i] = currentContext[(i + 1)];
                }
                currentContext[(depth - 1)] = obs;
            }
        }
    }

    public void checkConsistency() {
        checkConsistency(root);
    }

    public void checkConsistency(Restaurant r) {
        if (!r.isEmpty()) {
            int childrenTables = 0;

            for (Restaurant child : r.values()) {
                childrenTables += child.tables();
                checkConsistency(child);
            }

            if (r.customers != childrenTables) {
                System.out.println(num++);
            }
        }
    }

    public void print() {
        print(root, new int[0]);
    }

    public void print(Restaurant r, int[] rKey) {
        for (Integer k : r.keySet()) {
            int[] cKey = new int[rKey.length + 1];
            System.arraycopy(rKey, 0, cKey, 1, rKey.length);
            cKey[0] = k.intValue();
            print(r.get(k), cKey);
        }

        System.out.print("[");
        for (int keyVar : rKey) {
            System.out.print(keyVar + " ");
        }
        System.out.println("]");

        r.printTableMap();
        System.out.println();
    }

    // These three methods are to help debug PDIA_DMM, which seems to have some issues with empty restaurants
    public int tablesByType(int[] context) {
        Restaurant r = getDontAdd(context);
        if (r == null) {
            return -1;
        } else {
            return r.tableMap.size();
        }
    }

    public void tablesByType() {
        int[] context = new int[depth];
        for (int i = 0; i < depth; i++) context[i] = -1;
        tablesByType(root,context,0);
        System.out.println("");
    }

    public void tablesByType(Restaurant r, int[] context, int d) {
        for (int i = 0; i < d; i++) System.out.print(context[i] + ", ");
        System.out.println(" -> " + r.tableMap.size());
        if (d < depth) {
            for (Integer i : r.keySet()) {
                context[d] = i;
                tablesByType(r.get(i),context,d+1);
            }
            context[d] = -1;
        }
    }
}
