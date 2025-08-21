package com.example.my_campus.Fragments;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.example.my_campus.R;
import com.example.my_campus.utility;

public class fragmentSelectednavigation extends Fragment {
    private WebView webView;
    private utility ut = new utility();


    public fragmentSelectednavigation() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_selectednavigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView title = view.findViewById(R.id.tittle);
        webView = view.findViewById(R.id.webView);

        // 1️⃣  Show the buffering / loading dialog
        ut.showBufferingDialog(requireContext(), "Loading files");

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ut.dismissBufferingDialog(); // ✅ Dismiss dialog when loading is done
            }
        });

        Bundle bundle = getArguments();
        if (bundle != null) {
            String selectedTittle = bundle.getString("navigation");
            String mapHtml = "";
            if (selectedTittle != null) {
                if (selectedTittle.equals("campus")) {
//                    title.setText("College Campus Map");

                    mapHtml = "<iframe src=\"https://www.google.com/maps/d/embed?mid=18jHIqa1O_Qq1jVM_4BmNHduUzxy5eUQ&ehbc=2E312F\" "
                            + "style=\"width:100%; height:100%; border:0;\" "
                            + "allowfullscreen "
                            + "loading=\"lazy\"></iframe>";

                } else if (selectedTittle.equals("hostel")) {
//                    title.setText("Hostel Map");

                    mapHtml = "<iframe src=\"https://www.google.com/maps/d/embed?mid=1UVDNtpbRMAO3g9PkZYjwHO1D09apikE&ehbc=2E312F\" "
                            + "style=\"width:100%; height:100%; border:0;\" "
                            + "allowfullscreen "
                            + "loading=\"lazy\"></iframe>";
                }
                // Proper HTML with viewport settings
                String htmlData = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                        "<style>html, body { margin:0; padding:0; height:100%; } " +
                        "iframe { width:100%; height:100%; border:0; }</style></head>" +
                        "<body>" + mapHtml + "</body></html>";

                webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null);
            }
        }
    }
}
