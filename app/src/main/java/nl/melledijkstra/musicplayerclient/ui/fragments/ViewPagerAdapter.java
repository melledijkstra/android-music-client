package nl.melledijkstra.musicplayerclient.ui.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * <p>Created by Melle Dijkstra on 17-4-2016</p>
 */
public class ViewPagerAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments;
    private String[] pageTitles = new String[] { "Music Player", "Youtube" };

    public ViewPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return this.fragments.get(position);
    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return null;
        //return position > pageTitles.length || position < 0 ? "<notitle>" : this.pageTitles[position];
    }
}
