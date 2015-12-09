package com.spoiledmilk.ibikecph.map;

public class ObservablePageInteger {

    private OnIntegerChangeListener listener;

    private int pageValue;

    public void setOnIntegerChangeListener(OnIntegerChangeListener listener) {
        this.listener = listener;
    }

    public int getPageValue() {
        return pageValue;
    }

    public void setPageValue(int pageValue) {
        this.pageValue = pageValue;

        if (listener != null) {
            listener.onIntegerChanged(pageValue);
        }
    }
}
