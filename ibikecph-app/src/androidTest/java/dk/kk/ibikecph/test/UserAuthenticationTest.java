package dk.kk.ibikecph.test;

import dk.kk.ibikecphlib.map.MapActivity;
import dk.kk.ibikecphlib.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.contrib.DrawerActions.open;

import static org.hamcrest.Matchers.allOf;

/**
 * Created by kraen on 17-04-16.
 * The class tests the authentication - logging in and out of the app.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class UserAuthenticationTest {

    @Rule
    public ActivityTestRule<MapActivity> mActivityRule = new ActivityTestRule<>(MapActivity.class);


    @Test
    public void logout_sameActivity() {
        // Slide out the drawer menu.
        onView(withId(R.id.drawer_layout))
                .perform(open());

        // Click the my-profile item
        onView(allOf(withParent(withId(R.id.menuListView)), withChild(withText("Min Profil"))))
                .perform(click());

    }
}
