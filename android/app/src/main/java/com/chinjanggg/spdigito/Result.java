package com.chinjanggg.spdigito;

public class Result {
    private int sys, dia, pulse;

    public Result(String sys, String dia, String pulse) {
        this.sys = Integer.parseInt(sys);
        this.dia = Integer.parseInt(dia);
        this.pulse = Integer.parseInt(pulse);
    }

    public int getSys() {
        return sys;
    }

    public void setSys(int sys) {
        this.sys = sys;
    }

    public int getDia() {
        return dia;
    }

    public void setDia(int dia) {
        this.dia = dia;
    }

    public int getPulse() {
        return pulse;
    }

    public void setPulse(int pulse) {
        this.pulse = pulse;
    }
}