/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.egh.bikehist.ui.masterdata;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.egh.bikehist.R;
import de.egh.bikehist.model.Bike;
import de.egh.bikehist.model.Tag;
import de.egh.bikehist.model.TagType;
import de.egh.bikehist.ui.ListCallbacks;

/**
 * A basic sample which shows how to use com.example.android.common.view.SlidingTabLayout
 * to display a custom {@link ViewPager} title strip which gives continuous feedback to the user
 * when scrolling.
 */
public class AdministratorFragment extends Fragment {

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }
        setCallbacks((Callbacks) activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        setCallbacks(null);
    }

    private static final String TAG = AdministratorFragment.class.getSimpleName();
    static final String LOG_TAG = "SlidingTabsBasicFragment";
    private Callbacks callbacks = sDummyCallbacks;

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {

        @Override
        public void tabChanged() {
            Log.w(TAG, "tabChanged() Dummy implementation!");
        }
    };
    /**
     * A custom {@link ViewPager} title strip which looks much like Tabs present in Android v4.0 and
     * above, but is designed to give continuous feedback to the user when scrolling.
     */
    private SlidingTabLayout mSlidingTabLayout;
    /**
     * A {@link ViewPager} which will be used in conjunction with the {@link SlidingTabLayout} above.
     */
    private ViewPager mViewPager;
    /**
     * Actual Class of the shown Entities
     */
    private SamplePagerAdapter pageAdapter;

    private void setCallbacks(Callbacks callbacks) {
        if (callbacks != null)
            this.callbacks = callbacks;
        else
            this.callbacks = sDummyCallbacks;
    }

    /**
     * Returns true, if Create action is performed. Use this method in onPrepareOptionsMenu() to decide
     * to enable Create action.
     */
    public boolean isCreateActionPerformed() {
        return pageAdapter.isCreateActionPerformed();
    }

    /**
     * Inflates the {@link View} which will be displayed by this {@link Fragment}, from the app's
     * resources.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_administrator, container, false);
    }

    /**
     * Returns a Value of MasterDataContract.Type.
     */
    public String getActualEntityType() {
        return pageAdapter.getActualEntityType();
    }

    // BEGIN_INCLUDE (fragment_onviewcreated)

    /**
     * This is called after the {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has finished.
     * Here we can pick out the {@link View}s we need to configure from the content view.
     * <p/>
     * We set the {@link ViewPager}'s adapter to be an instance of {@link SamplePagerAdapter}. The
     * {@link SlidingTabLayout} is then given the {@link ViewPager} so that it can populate itself.
     *
     * @param view View created in {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // BEGIN_INCLUDE (setup_viewpager)
        // Get the ViewPager and set it's PagerAdapter so that it can display items
        mViewPager = (ViewPager) view.findViewById(R.id.viewpager);
        pageAdapter = new SamplePagerAdapter();
        mViewPager.setAdapter(pageAdapter);

        // END_INCLUDE (setup_viewpager)

        // BEGIN_INCLUDE (setup_slidingtablayout)
        // Give the SlidingTabLayout the ViewPager, this must be done AFTER the ViewPager has had
        // it's PagerAdapter set.
        mSlidingTabLayout = (SlidingTabLayout) view.findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setCallbacks(new SlidingTabLayout.Callbacks() {
            @Override
            public void onPageSet(int position) {
//                Log.v(TAG, "onPageSet");
                callbacks.tabChanged();
            }
        });
        // END_INCLUDE (setup_slidingtablayout)
    }
    // END_INCLUDE (fragment_onviewcreated)

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        pageAdapter.close();

    }

    /**
     * Call this after data has been changed in the details edit mode.
     */
    public void refreshUI() {

        pageAdapter.refreshUI();

    }

    /**
     * For the consumer Activity.
     */
    public interface Callbacks {
        /**
         * Tab has changed.
         */
        void tabChanged();
    }

    /**
     * The {@link android.support.v4.view.PagerAdapter} used to display pages in this sample.
     * The individual pages are simple and just display two lines of text. The important section of
     * this class is the {@link #getPageTitle(int)} method which controls what is displayed in the
     * {@link SlidingTabLayout}.
     */
    class SamplePagerAdapter extends PagerAdapter {


        // Manages the List content for all three master data types.
        private MasterDataListController masterDataListControllerBike;
        private MasterDataListController masterDataListControllerTagType;
        private MasterDataListController masterDataListControllerTag;

        /**
         * Returns a Value of MasterDataContract.Type.
         */
        public String getActualEntityType() {
            switch (mViewPager.getCurrentItem()) {
                case 0:
                    return MasterDataContract.Type.Values.BIKE;
                case 1:
                    return MasterDataContract.Type.Values.TAG_TYPE;
                case 2:
                    return MasterDataContract.Type.Values.TAG;
            }
            return null;
        }

        /**
         * Returns true, if Create action is performed. Use this method in onPrepareOptionsMenu() to
         * decide
         * to enable Create action.
         */
        public boolean isCreateActionPerformed() {
            switch (mViewPager.getCurrentItem()) {
                case 0:
                    return masterDataListControllerBike.isCreateActionPerformed();
                case 1:
                    return masterDataListControllerTagType.isCreateActionPerformed();
                case 2:
                    return masterDataListControllerTag.isCreateActionPerformed();
            }
            return false;
        }

        /**
         * @return the number of pages to display
         */
        @Override
        public int getCount() {
            return 3;
        }

        /**
         * @return true if the value returned from {@link #instantiateItem(ViewGroup, int)} is the
         * same object as the {@link View} added to the {@link ViewPager}.
         */
        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        // BEGIN_INCLUDE (pageradapter_getpagetitle)

        /**
         * Return the title of the item at {@code position}. This is important as what this method
         * returns is what is displayed in the {@link SlidingTabLayout}.
         * <p/>
         * Here we construct one using the position value, but for real application the title should
         * refer to the item's contents.
         */
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.bikes);
                case 1:
                    return getString(R.string.tagTypes);
                case 2:
                    return getString(R.string.tags);
            }
            return "Item " + (position + 1);
        }
        // END_INCLUDE (pageradapter_getpagetitle)

        public void refreshUI() {
            if (masterDataListControllerBike != null)
                masterDataListControllerBike.restart();
            if (masterDataListControllerTagType != null)
                masterDataListControllerTagType.restart();
            if (masterDataListControllerTag != null)
                masterDataListControllerTag.restart();
        }


        /**
         * Instantiate the {@link View} which should be displayed at {@code position}. Here we
         * inflate a layout from the apps resources and then change the text view to signify the position.
         */
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View view = null;
            switch (position) {
                case 0:
                    if (masterDataListControllerBike == null) {
                        masterDataListControllerBike = new MasterDataListController(getActivity(), container,
                                getActivity().getLayoutInflater(), getLoaderManager(), Bike.class);

                        masterDataListControllerBike.setListCallbacks((ListCallbacks) getActivity());

                        // Inflate a new layout from our resources
                    }
                    view = masterDataListControllerBike.getView();
                    break;
                case 1:
                    if (masterDataListControllerTagType == null) {
                        masterDataListControllerTagType = new MasterDataListController(getActivity(), container,
                                getActivity().getLayoutInflater(), getLoaderManager(), TagType.class);

                        masterDataListControllerTagType.setListCallbacks((ListCallbacks) getActivity());

                    }
                    // Inflate a new layout from our resources
                    view = masterDataListControllerTagType.getView();
                    break;
                case 2:
                    if (masterDataListControllerTag == null) {
                        masterDataListControllerTag = new MasterDataListController(getActivity(), container,
                                getActivity().getLayoutInflater(), getLoaderManager(), Tag.class);

                        masterDataListControllerTag.setListCallbacks((ListCallbacks) getActivity());

                    }
                    // Inflate a new layout from our resources
                    view = masterDataListControllerTag.getView();
                    break;
                default:
                    Log.e(TAG, "Unknown position number " + position);
                    return null;
            }
            // Add the newly created View to the ViewPager
            container.addView(view);

            // Return the View
            return view;
        }

        /**
         * Destroy the item from the {@link ViewPager}. In our case this is simply removing the
         * {@link View}.
         */
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
//            masterDataListControllerBike.close();
        }

        public void close() {
            if (masterDataListControllerBike != null)
                masterDataListControllerBike.close();
            if (masterDataListControllerTag != null)
                masterDataListControllerTag.close();
            if (masterDataListControllerTagType != null)
                masterDataListControllerTagType.close();
        }
    }
}
