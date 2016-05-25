package com.spoiledmilk.ibikecph.introduction;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.spoiledmilk.ibikecph.IBikeApplication;
import com.spoiledmilk.ibikecph.R;

/**
 * Created by kraen on 22-05-16.
 */
public class GreenPathsIntroductionActivity extends IntroductionActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageView image = (ImageView) findViewById(R.id.introductionImage);
        image.setImageResource(R.drawable.green_paths_top_image);

        TextView title = (TextView) findViewById(R.id.introductionTitle);
        title.setText(IBikeApplication.getString("introduction_greenest_route_header_ibc"));

        TextView body = (TextView) findViewById(R.id.introductionBody);
        body.setText(IBikeApplication.getString("introduction_greenest_route_body_ibc"));

        ImageView footerImage = (ImageView) findViewById(R.id.introductionFooterImage);
        footerImage.setImageResource(R.drawable.btn_route_green_enabled);

        TextView footer = (TextView) findViewById(R.id.introductionFooter);
        footer.setText(IBikeApplication.getString("introduction_greenest_route_footer_ibc"));
    }
}
