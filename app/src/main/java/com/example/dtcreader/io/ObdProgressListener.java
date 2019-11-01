package com.example.dtcreader.io;

public interface ObdProgressListener {
    void stateUpdate(final ObdCommandJob job);
}
