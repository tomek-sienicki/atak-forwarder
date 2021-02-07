package com.paulmandal.atak.forwarder.tracker;

import android.content.Context;

import com.atakmap.android.maps.MapView;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.paulmandal.atak.forwarder.Config;
import com.paulmandal.atak.forwarder.channel.TrackerUserInfo;
import com.paulmandal.atak.forwarder.channel.UserTracker;
import com.paulmandal.atak.forwarder.handlers.InboundMessageHandler;
import com.paulmandal.atak.forwarder.plugin.Destroyable;
import com.paulmandal.atak.libcotshrink.protobuf.ContactProtobufConverter;
import com.paulmandal.atak.libcotshrink.protobuf.TakvProtobufConverter;
import com.paulmandal.atak.libcotshrink.protobufs.ProtobufContact;
import com.paulmandal.atak.libcotshrink.protobufs.ProtobufTakv;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.paulmandal.atak.forwarder.cotutils.CotMessageTypes.TYPE_PLI;

public class TrackerCotGenerator implements UserTracker.TrackerUpdateListener, Destroyable {
    private static final String TAG = Config.DEBUG_TAG_PREFIX + TrackerCotGenerator.class.getSimpleName();

    private static final int STALE_TIME_OFFSET_MS = 75000;
    private static final double UNKNOWN_LE_CE = 9999999.0;

    private static final String TAG_DETAIL = "detail";
    private static final String TAG_UID = "uid";
    private static final String TAG_DROID = "Droid";
    private static final String TAG_PRECISION_LOCATION = "precisionlocation";
    private static final String TAG_ALTSRC = "altsrc";
    private static final String TAG_GEOPOINTSRC = "geopointsrc";
    private static final String TAG_GROUP = "__group";
    private static final String TAG_ROLE = "role";
    private static final String TAG_NAME = "name";
    private static final String TAG_STATUS = "status";
    private static final String TAG_BATTERY = "battery";

    private static final String VALUE_UID_PREFIX = "MESHTASTIC";
    private static final String VALUE_MESHTASTIC_DEVICE = "Meshtastic Device";
    private static final String VALUE_ATAK_FORWARDER = "ATAK Forwarder";
    private static final String VALUE_HOW_GPS = "m-g";
    private static final String VALUE_UNKNOWN = "???";
    private static final String VALUE_GPS = "GPS";

    private static final int WHITE_INDEX = 0;
    private static final int TEAM_MEMBER_INDEX = 0;

    public static String[] TEAMS = {
            "White",
            "Yellow",
            "Orange",
            "Magenta",
            "Red",
            "Maroon",
            "Purple",
            "Dark Blue",
            "Blue",
            "Cyan",
            "Teal",
            "Green",
            "Dark Green",
            "Brown"
    };

    public static String[] ROLES = {
            "Team Member",
            "Team Lead",
            "HQ",
            "Sniper",
            "Medic",
            "Forward Observer",
            "RTO",
            "K9"
    };

    private InboundMessageHandler mInboundMessageHandler;

    private List<TrackerUserInfo> mTrackers;
    private final String mPluginVersion;

    private ScheduledExecutorService mWorkerExecutor;
    private boolean mDestroyCalled = false;

    public TrackerCotGenerator(List<Destroyable> destroyables, UserTracker userTracker, InboundMessageHandler inboundMessageHandler, String pluginVersion) {
        mInboundMessageHandler = inboundMessageHandler;
        mPluginVersion = pluginVersion;

        destroyables.add(this);
        userTracker.addTrackerUpdateListener(this);

        startWorkerThread();
    }

    @Override
    public void trackersUpdated(List<TrackerUserInfo> trackers) {
        mTrackers = trackers;
        drawTrackers();
    }

    @Override
    public void onDestroy(Context context, MapView mapView) {
        mDestroyCalled = true;
    }

    private void startWorkerThread() {
        mWorkerExecutor = Executors.newSingleThreadScheduledExecutor();
        mWorkerExecutor.scheduleAtFixedRate(() -> {
            while (!mDestroyCalled) {
                drawTrackers();
            }
        }, 0, 5, TimeUnit.MINUTES);
    }

    private void drawTrackers() {
        for (TrackerUserInfo tracker : mTrackers) {
            drawTracker(tracker);
        }
    }

    private void drawTracker(TrackerUserInfo tracker) {
        if (!tracker.gpsValid) {
            // Ignore updates that don't contain a valid GPS point
            return;
        }

        CotEvent spoofedPli = new CotEvent();

        long lastMsgTime = System.currentTimeMillis() - tracker.lastSeenTime;
        CoordinatedTime lastMsgCoordinatedTime = new CoordinatedTime(lastMsgTime);
        CoordinatedTime staleCoordinatedTime = new CoordinatedTime(lastMsgTime + STALE_TIME_OFFSET_MS);

        spoofedPli.setUID(String.format("%s-%s", VALUE_UID_PREFIX, tracker.meshId.replaceAll("!", "")));
        spoofedPli.setType(TYPE_PLI);
        spoofedPli.setTime(lastMsgCoordinatedTime);
        spoofedPli.setStart(lastMsgCoordinatedTime);
        spoofedPli.setStale(staleCoordinatedTime);
        spoofedPli.setHow(VALUE_HOW_GPS);
        spoofedPli.setPoint(new CotPoint(tracker.lat, tracker.lon, tracker.altitude, UNKNOWN_LE_CE, UNKNOWN_LE_CE));

        CotDetail cotDetail = new CotDetail(TAG_DETAIL);

        TakvProtobufConverter mTakvProtobufConverter = new TakvProtobufConverter();

        ProtobufTakv.Takv.Builder takv = ProtobufTakv.Takv.newBuilder();
        takv.setOs(1);
        takv.setVersion(mPluginVersion);
        takv.setDevice(VALUE_MESHTASTIC_DEVICE);
        takv.setPlatform(VALUE_ATAK_FORWARDER);
        mTakvProtobufConverter.maybeAddTakv(cotDetail, takv.build());

        ProtobufContact.Contact.Builder contact = ProtobufContact.Contact.newBuilder();
        contact.setCallsign(tracker.callsign);
        ContactProtobufConverter mContactProtobufConverter = new ContactProtobufConverter();
        mContactProtobufConverter.maybeAddContact(cotDetail, contact.build(), false);

        CotDetail uidDetail = new CotDetail(TAG_UID);
        uidDetail.setAttribute(TAG_DROID, tracker.callsign);
        cotDetail.addChild(uidDetail);

        CotDetail precisionLocationDetail = new CotDetail(TAG_PRECISION_LOCATION);
        precisionLocationDetail.setAttribute(TAG_ALTSRC, VALUE_UNKNOWN);
        precisionLocationDetail.setAttribute(TAG_GEOPOINTSRC, VALUE_GPS);
        cotDetail.addChild(precisionLocationDetail);

        String team = TEAMS[WHITE_INDEX];
        String role = ROLES[TEAM_MEMBER_INDEX];
        if (tracker.shortName != null && tracker.shortName.length() > 1) {
            try {
                // Try to split the shortname and get team/role values
                int roleIndex = Integer.parseInt(tracker.shortName.substring(0, 1));
                int teamIndex = Integer.parseInt(tracker.shortName.substring(1));

                if (teamIndex > -1 && teamIndex < TEAMS.length) {
                    team = TEAMS[teamIndex];
                }

                if (roleIndex > -1 && roleIndex < ROLES.length) {
                    role = ROLES[roleIndex];
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        CotDetail groupDetail = new CotDetail(TAG_GROUP);
        groupDetail.setAttribute(TAG_ROLE, role);
        groupDetail.setAttribute(TAG_NAME, team);
        cotDetail.addChild(groupDetail);

        if (tracker.batteryPercentage != null) {
            CotDetail statusDetail = new CotDetail(TAG_STATUS);
            statusDetail.setAttribute(TAG_BATTERY, Integer.toString(tracker.batteryPercentage));
            cotDetail.addChild(statusDetail);
        }

        spoofedPli.setDetail(cotDetail);

        mInboundMessageHandler.retransmitCotToLocalhost(spoofedPli);
    }
}
