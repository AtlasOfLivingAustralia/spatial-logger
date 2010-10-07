package org.ala.spatial.analysis.web;

import au.org.emii.portal.composer.MapComposer;
import au.org.emii.portal.composer.UtilityComposer;
import au.org.emii.portal.menu.MapLayer;
import au.org.emii.portal.menu.MapLayerMetadata;
import au.org.emii.portal.settings.SettingsSupplementary;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.zkoss.zhtml.Filedownload;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Button;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListModelArray;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Tabbox;
import org.apache.log4j.Logger;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Row;

/**
 *
 * @author adam
 */
public class FilteringResultsWCController extends UtilityComposer {

    private static Logger logger = Logger.getLogger(FilteringResultsWCController.class);


    private static final String SAT_URL = "sat_url";
    public Button download;
    public Button downloadsamples;
    public Button refreshButton2;
    public Button mapspecies;
    public Listbox popup_listbox_results;
    public Label results_label;
    //public Label results_label2;
    public Label results_label2_occurrences;
    public Label results_label2_species;
    //public Label results_label_extra;
    public int results_pos;
    public String[] results = null;
    public String pid;
    String shape;
    private String satServer;
    private SettingsSupplementary settingsSupplementary = null;

    Row rowUpdating;
    Row rowCounts;

    int results_count = 0;
    int results_count_occurrences = 0;

    @Override
    public void afterCompose() {
        super.afterCompose();

        // onClick$refreshButton2();


        StringBuffer sb = new StringBuffer();
        sb.append("{\"eventTypeId\": 123,");
        sb.append("\"comment\": \"For doing some research with..\",");
        sb.append("\"userEmail\" : \"waiman.mok@csiro.au\",");
        sb.append("\"recordCounts\" : {");
        sb.append("\"dp123\": 32,");
        sb.append("\"dr143\": 22,");
        sb.append("\"ins322\": 55 } }");
        logger.debug(sb.toString());
        //logger.warn(sb.toString());
        //logger.info(sb.toString());

        //log to remote ala-logger
        //logger.d;

    }
    boolean addedListener = false;

    @Override
    public void redraw(Writer out) throws java.io.IOException {
        super.redraw(out);
        
        //results_label_extra.setValue("    [Updating...]");
        setUpdatingCount(true);

        System.out.println("redraw:filteringresultswccontroller");
        if (!addedListener) {
            addedListener = true;
            //register for viewport changes
            EventListener el = new EventListener() {

                public void onEvent(Event event) throws Exception {
                    // refresh count may be required if area is
                    // not an envelope.
                    String area = getMapComposer().getSelectionArea();
                    if (!area.contains("ENVELOPE(")) {
                        refreshCount();
                    }
                }
            };
            getMapComposer().getLeftmenuSearchComposer().addViewportEventListener("filteringResults", el);

        }
            //results_label_extra.setValue("");
    }

    void setUpdatingCount(boolean set){
        rowUpdating.setVisible(set);
        rowCounts.setVisible(!set);
    }

    public void populateList() {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/list");

            String out = postInfo(sbProcessUrl.toString());
            //remove trailing ','
            if (out.length() > 0 && out.charAt(out.length() - 1) == ',') {
                out = out.substring(0, out.length() - 1);
            }
            results = out.split("\\|");
            java.util.Arrays.sort(results);

            if (results.length == 0 || results[0].trim().length() == 0) {
                //results_label.setValue("no species in area");
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                results = null;
                popup_listbox_results.setVisible(false);
                results_label.setVisible(false);
                refreshButton2.setVisible(true);
                return;
            }

            // results should already be sorted: Arrays.sort(results);
            int length = results.length;
            String[] tmp = results;
            if (results.length > 200) {
                tmp = java.util.Arrays.copyOf(results, 200);
                results_label.setValue("preview of first 200 species found");
            }else {
                results_label.setValue("preview of all " + results.length + " species found");
            }

            popup_listbox_results.setModel(new ListModelArray(tmp, false));
            popup_listbox_results.setItemRenderer(
                    new ListitemRenderer() {

                        public void render(Listitem li, Object data) {
                            String s = (String) data;
                            String[] ss = s.split("[*]");


                            Listcell lc = new Listcell(ss[0]);
                            lc.setParent(li);

                            if (ss.length > 1) {
                                lc = new Listcell(ss[1]);
                                lc.setParent(li);
                            }

                            if (ss.length > 2) {
                                lc = new Listcell(ss[2]);
                                lc.setParent(li);
                            }

                            if (ss.length > 3) {
                                lc = new Listcell(ss[3]);
                                lc.setParent(li);
                            }
                        }
                    });
            if (length < 200) {
                //results_label.setValue("species in active area: " + length);
                //results_label2.setValue(length + "");
            } else {
                //results_label.setValue("species in active area: " + length + " (first 200 listed)");
                //results_label2.setValue(length + "");
                //results_label_extra.setValue(" (first 200 listed)");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean isTabOpen() {
        try {
            Tabbox analysisOptsTabbox =
                    (Tabbox) getParent() //htmlmacrocomponent
                    .getParent() //tabpanel
                    .getParent() //tabpanels
                    .getParent(); //tabbox

            if (analysisOptsTabbox.getSelectedTab().getIndex() == 0) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void refreshCount() {
        //check if tab is open
        if (!isTabOpen() || !updateParameters()) {
            return;
        }
        
        //results_label2.setValue("    [Updating...]");
        setUpdatingCount(true);
        Events.echoEvent("onRefreshCount", this, null);
    }

    public void onRefreshCount(Event e) throws Exception {
        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/species/count");

            String []out = postInfo(sbProcessUrl.toString()).split("\n");

            results_count = Integer.parseInt(out[0]);
            results_count_occurrences = Integer.parseInt(out[1]);
            if (results_count == 0) {
                //results_label.setValue("no species in active area");
                results_label2_species.setValue("0");
                results_label2_occurrences.setValue("0");
                results = null;
                popup_listbox_results.setVisible(false);
                results_label.setVisible(false);
                refreshButton2.setVisible(true);
                return;
            }

            //results_label.setValue("species in active area: " + results_count);
            //results_label2.setValue(results_count + " (" + results_count_occurrences + " occurrences)");
            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
            setUpdatingCount(false);

            //hide results list, show 'preview list' button
            popup_listbox_results.setVisible(false);
            results_label.setVisible(false);
            refreshButton2.setVisible(true);

            // toggle the map button
            if (results_count > 0 && results_count_occurrences <= 100000) {
                mapspecies.setVisible(true);
            } else {
                mapspecies.setVisible(false);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onClick$download() {
        //results_label_extra.setValue("    [Generating download...]");
        //update 'results'
        if (updateParameters()
                || !popup_listbox_results.isVisible()) {
            populateList();
        }

        StringBuffer sb = new StringBuffer();
        sb.append("Family Name,Scientific Name,Common name\\s,Taxon rank\r\n");
        for (String s : results) {
            sb.append("\"");
            sb.append(s.replaceAll("\\*", "\",\""));
            sb.append("\"");
            sb.append("\r\n");
        }
        //results_label_extra.setValue("");
        String spid = pid;
        if(spid == null || spid.equals("none")){
            spid = String.valueOf(System.currentTimeMillis());
        }

        SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
        String sdate = date.format(new Date());

        Filedownload.save(sb.toString(), "text/plain", "Species_list_" + sdate + "_" + spid + ".csv");
    }

    public void onClick$downloadsamples() {
          //  results_label_extra.setValue("    [Generating download...]");
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        try {
            StringBuffer sbProcessUrl = new StringBuffer();
            sbProcessUrl.append("/filtering/apply");
            sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
            sbProcessUrl.append("/samples/list");

            String samplesfile = postInfo(sbProcessUrl.toString());

            //results_label_extra.setValue("");

            URL u = new URL(satServer + "/alaspatial/" + samplesfile);
            SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd");
            String sdate = date.format(new Date());

            String spid = pid;
            if(spid == null || spid.equals("none")){
                spid = String.valueOf(System.currentTimeMillis());
            }

            Filedownload.save(u.openStream(), "application/zip", u.getFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClick$mapspecies() {
            //results_label_extra.setValue("    [Mapping...]");
            //Events.echoEvent("onMapSpecies", this, null);
        onMapSpecies(null);
    }

    public void onMapSpecies(Event event){
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        try {
            String area = getMapComposer().getViewArea();

            StringBuffer sbProcessUrl = new StringBuffer();
            if(!getMapComposer().useClustering()){
                sbProcessUrl.append("/filtering/apply");
                sbProcessUrl.append("/pid/" + URLEncoder.encode(pid, "UTF-8"));
                sbProcessUrl.append("/samples/geojson");
                sbProcessUrl.append("?area=").append(URLEncoder.encode(area,"UTF-8"));
                String slist = getInfo(sbProcessUrl.toString());
                //getMapComposer().addGeoJSONLayer("Species in Active area", satServer + "/alaspatial/" + geojsonfile);                               
                System.out.println("onMapSpecies: " + slist);
                String [] results = slist.split("\n");
                getMapComposer().addGeoJSONLayerProgressBar("Species in Active area", satServer + "/alaspatial/" + results[0], "", false, Integer.parseInt(results[1]), null);//set progress bar with maximum
            }else{
                MapLayerMetadata md = new MapLayerMetadata();
                md.setLayerExtent(area, 0.2);

                sbProcessUrl.append("species");
                sbProcessUrl.append("/cluster/area/").append(URLEncoder.encode(area,"UTF-8"));
                String id = String.valueOf(System.currentTimeMillis());
                sbProcessUrl.append("/id/").append(URLEncoder.encode(id,"UTF-8"));
                sbProcessUrl.append("/now");
                sbProcessUrl.append("?z=").append(String.valueOf(getMapComposer().getMapZoom()));
                sbProcessUrl.append("&a=").append(URLEncoder.encode(md.getLayerExtentString(),"UTF-8"));
                MapLayer ml = getMapComposer().addGeoJSONLayer("Species in Active area", satServer + "/alaspatial/" + sbProcessUrl.toString());

                if(ml.getMapLayerMetadata() == null){
                    ml.setMapLayerMetadata(new MapLayerMetadata());
                }
                ml.getMapLayerMetadata().setLayerExtent(area, 0.2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }

        try {
            HttpClient client = new HttpClient();

            GetMethod get = new GetMethod(satServer + "/alaspatial/ws" + urlPart); // testurl
            get.addRequestHeader("Accept", "application/json, text/javascript, */*");

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    private String postInfo(String urlPart) {
        if (settingsSupplementary != null) {
            satServer = settingsSupplementary.getValue(SAT_URL);
        }
        try {
            HttpClient client = new HttpClient();

            PostMethod get = new PostMethod(satServer + "/alaspatial/ws" + urlPart); // testurl

            get.addRequestHeader("Accept", "application/json, text/javascript, */*");
            get.addParameter("area", URLEncoder.encode(shape, "UTF-8"));

            System.out.println("satServer:" + satServer + " ** postInfo:" + urlPart + " ** " + shape);

            int result = client.executeMethod(get);

            //TODO: confirm result
            String slist = get.getResponseBodyAsString();

            return slist;
        } catch (Exception ex) {
            //TODO: error message
            System.out.println("getInfo.error:");
            ex.printStackTrace(System.out);
        }
        return null;
    }

    public void onClick$refreshButton2() {
        if (!isTabOpen()) {
            return;
        }
        if (updateParameters()
                || !popup_listbox_results.isVisible()) {
            populateList();

            //show update list if count > 0
            refreshButton2.setVisible(false);
            popup_listbox_results.setVisible(true);
            results_label.setVisible(true);
        }
    }

    boolean updateParameters() {
        //extract 'shape' and 'pid' from composer
        String area = getMapComposer().getSelectionArea();

        if (area.contains("ENVELOPE(")) {
            shape = "none";
            pid = area.substring(9, area.length() - 1);
            return true;
        } else {
            pid = "none";
            if (shape != area) {
                shape = area;
                return true;
            } else {
                return false;
            }
        }
    }

    static public void open() {
        FilteringResultsWCController win = (FilteringResultsWCController) Executions.createComponents(
                "/WEB-INF/zul/AnalysisFilteringResults.zul", null, null);
        try {
            win.doModal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void refreshCount(int newCount, int newOccurrencesCount) {
        //results_label_extra.setValue("    [Updating...]");
        results_count = newCount;
        results_count_occurrences = newOccurrencesCount;
        if (results_count == 0) {
            //results_label.setValue("no species in active area");
            results_label2_species.setValue(String.valueOf(results_count));
            results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
            results = null;
            popup_listbox_results.setVisible(false);
            results_label.setVisible(false);
            refreshButton2.setVisible(true);
        }

        //results_label.setValue("species in active area: " + results_count);
        results_label2_species.setValue(String.valueOf(results_count));
        results_label2_occurrences.setValue(String.valueOf(results_count_occurrences));
        setUpdatingCount(false);

        //hide results list, show 'preview list' button
        popup_listbox_results.setVisible(false);
        results_label.setVisible(false);
        refreshButton2.setVisible(true);
        
        // toggle the map button
        if (results_count > 0 && results_count <= 100000) {
            mapspecies.setVisible(true);
        } else {
            mapspecies.setVisible(false);
        }
    }
}
