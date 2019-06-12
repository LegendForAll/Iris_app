package com.example.hothaingoc.flower_cv_a1;

import android.graphics.Bitmap;
import android.graphics.RectF;

import java.util.List;

public interface Classifier {

    List<Recognition> recognizeImage(Bitmap bitmap);

    void enableStatLogging(boolean logStats);

    String getStatString();

    void close();

    //recognition objects
    public class Recognition{

        private final String id;
        private final String title;
        private final Float confidence;
        private RectF location;

        public Recognition(final String id, final String title, final Float confidence, final RectF location) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
            this.location = location;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public Float getConfidence() {
            return confidence;
        }

        public RectF getLocation() {
            return location;
        }

        public void setLocation(RectF location) {
            this.location = location;
        }

        @Override
        public String toString() {
            String resultString = "";

            if(this.id != null)
                resultString += "[" + id + "] ";
            if(this.title != null)
                resultString += title + " ";
            if(this.confidence != null)
                resultString += String.format("(%.1f%%) ", this.confidence * 100.0f);
            if(this.location != null)
                resultString += location + " ";

            return  resultString.trim();
        }
    }
}
