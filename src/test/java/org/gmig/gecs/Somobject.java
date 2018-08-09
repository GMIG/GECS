package org.gmig.gecs;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by brix isOn 2/8/2018.
 */
public class Somobject implements Serializable {
    int a ;
    String b;

    @Override
    public int hashCode() {
        return a+b.hashCode();
    }

    Somobject(int a,String b) {
        this.a = a;
        this.b = b;
    }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!Somobject.class.isAssignableFrom(obj.getClass())) {
                return false;
            }

            Somobject oo = (Somobject) obj;
            return(a==oo.a && Objects.equals(b, oo.b));
        }
}
