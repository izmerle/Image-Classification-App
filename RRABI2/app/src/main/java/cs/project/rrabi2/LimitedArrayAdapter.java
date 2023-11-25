package cs.project.rrabi2;

import android.content.Context;
import android.widget.ArrayAdapter;

public class LimitedArrayAdapter extends ArrayAdapter<CharSequence> {
    private int maxItemsToShow;

    public LimitedArrayAdapter(Context context, int resource, int textViewResourceId, CharSequence[] objects, int maxItemsToShow) {
        super(context, resource, textViewResourceId, objects);
        this.maxItemsToShow = maxItemsToShow;
    }

    @Override
    public int getCount() {
        int count = super.getCount();
        return Math.min(count, maxItemsToShow);
    }
}
