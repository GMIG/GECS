package org.gmig.gecs.groups;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by brix on 1/15/2019.
 */
public class VisModule extends Module {
    private ManagedDevice videoSource = null;
    private Set<ManagedDevice> visualisers = new HashSet<>();

    private VisModule(VisModuleBuilder b) {
        super(b);
        videoSource = b.videoSource;
        visualisers = b.visualisers;
    }

    public static VisModuleBuilder newBuilder(){
        return new VisModuleBuilder();
    }

    public ManagedDevice getSource(){
        return videoSource;
    }

    public Set<ManagedDevice> getVisualisers(){
        return Collections.unmodifiableSet(visualisers);
    }

    public static class VisModuleBuilder extends Module.ModuleBuilder<VisModuleBuilder>{
        private ManagedDevice videoSource = null;
        private Set<ManagedDevice> visualisers = new HashSet<>();

        public VisModuleBuilder setVideoSource(ManagedDevice videoSource){
            this.videoSource = videoSource;
           return this;
        }

        public VisModuleBuilder addVisualiser(ManagedDevice vizualiser){
            visualisers.add(vizualiser);
            return this;
        }

        @Override
        public VisModuleBuilder addSwitchable(int where, String id, Switchable sw){
            throw new IllegalArgumentException("Adding Switchables is not permitted");
        }

        @Override
        public VisModule build() {
            if(videoSource==null)
                throw new IllegalArgumentException("Video source not set");
            super.addSwitchable(1, videoSource.getName(), videoSource);
            visualisers.forEach((v)->super.addSwitchable(0,v.getName(),v));
            super.build();
            return new VisModule(this);
        }
    }

}
