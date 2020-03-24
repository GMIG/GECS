package org.gmig.gecs.device;

/**
 * Created by brix isOn 2/27/2018.
 */
public class StateRequestResult {
    @Override
    public boolean equals(Object o){
        if(!(o instanceof StateRequestResult))
            return false;
        return ((StateRequestResult)o).isOn() == isOn();
    }

    public boolean isOn(){
        return state;
    }
    private boolean state;

    public Object returned() {
        return returned;
    }
    private Object returned;

     private StateRequestResult(boolean state, Object returned){
        this.state = state;
        this.returned = returned;
    }
    static public StateRequestResult IsOn(Object o){
        return new StateRequestResult(true,o);
    }
    static public StateRequestResult IsOff(Object o){
        return new StateRequestResult(false,o);
    }

    @Override
    public String toString(){
        if(isOn())
            return "is on";
        else
            return "is off";

    }

}
