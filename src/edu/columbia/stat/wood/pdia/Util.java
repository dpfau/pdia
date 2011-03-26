/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.columbia.stat.wood.pdia;

import edu.columbia.stat.wood.hpyp.Restaurant;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author davidpfau
 */
public class Util {

    public static <E> E copy(E orig) {
        Object obj = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));

            obj = in.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return (E) obj;
    }

    public static int[] randPermute(int n) {
        int[] order = new int[n];
        LinkedList<Integer> ind = new LinkedList<Integer>();
        for (int i = 0; i < n; i++) {
            ind.addLast(i);
        }
        for (int i = 0; i < n; i++) {
            order[i] = ind.remove(Restaurant.RNG.nextInt(n - i));
        }
        return order;
    }

    public static Object[] randArray(Collection in) {
        int[] order = randPermute(in.size());
        Object[] randArray = new Object[in.size()];
        int i = 0;
        for (Object o : in) {
            randArray[order[i++]] = o;
        }
        return randArray;
    }

    public static int sum(int[] arr) {
        int s = 0;
        for (int i = 0; i < arr.length; i++) {
            s += arr[i];
        }
        return s;
    }

    public static double sum(double[] arr) {
        double s = 0;
        for (int i = 0; i < arr.length; i++) {
            s += arr[i];
        }
        return s;
    }

    public static double scoreToLogLoss(double[] score) {
        double logLoss = 0.0;
        for (double d : score) {
            logLoss += Math.log(d);
        }
        return -logLoss/score.length/Math.log(2.0);
    }

    public static void addArrays(double[] base, double[] other) {
        assert (base.length == other.length);
        for (int i = 0; i < base.length; i++) {
            base[i] += other[i];
        }
    }

    public static int totalLen(int[][] data) {
        int len = 0;
        for (int i = 0; i < data.length; i++) {
            len += data[i].length;
        }
        return len;
    }
}
