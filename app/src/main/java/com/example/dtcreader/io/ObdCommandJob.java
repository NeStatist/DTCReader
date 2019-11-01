package com.example.dtcreader.io;

import com.github.pires.obd.commands.ObdCommand;

public class ObdCommandJob {
    private Long _id;
    private ObdCommand _command;
    private ObdCommandJobState _state;


    public ObdCommandJob(ObdCommand command) {
        _command = command;
        _state = ObdCommandJobState.NEW;
    }

    Long getId() {
        return _id;
    }

    void setId(Long id) {
        _id = id;
    }

    public ObdCommand getCommand() {
        return _command;
    }

    public ObdCommandJobState getState() {
        return _state;
    }


    void setState(ObdCommandJobState state) {
        _state = state;
    }


    public enum ObdCommandJobState {
        NEW,
        RUNNING,
        EXECUTION_ERROR,
        BROKEN_PIPE,
        QUEUE_ERROR,
        NOT_SUPPORTED
    }

}