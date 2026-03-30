package com.perf2gerber.model;

public class FixedComponent extends Component {
    private String pinoutOrCount;

    public FixedComponent() {}

    public String getPinoutOrCount() { return pinoutOrCount; }
    public void setPinoutOrCount(String pinoutOrCount) { this.pinoutOrCount = pinoutOrCount; }
}
