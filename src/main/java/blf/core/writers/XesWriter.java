package blf.core.writers;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import blf.core.exceptions.ProgramException;
import blf.util.TypeUtils;
import io.reactivex.annotations.NonNull;
import org.deckfour.xes.model.XAttributable;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeBooleanImpl;
import org.deckfour.xes.model.impl.XAttributeContinuousImpl;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeListImpl;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XAttributeTimestampImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.deckfour.xes.out.XesXmlSerializer;

/**
 * XesExporter
 */
public class XesWriter extends DataWriter {
    private static final Logger LOGGER = Logger.getLogger(XesWriter.class.getName());
    private static final String PID_ATTRIBUTE = "ident:pid";
    private static final String PIID_ATTRIBUTE = "ident:piid";
    private static final String EID_ATTRIBUTE = "ident:eid";
    private static final String DEFAULT_PID = "pid0";
    private static final String DEFAULT_PIID = "piid0";
    private static final String DEFAULT_EID = "eid";
    private static long eid = 0;

    private final Map<String, Map<String, XTrace>> traces;
    private final Map<String, Map<String, Map<String, XEvent>>> events;
    private XAttributable element;

    public XesWriter() {
        this.traces = new LinkedHashMap<>();
        this.events = new LinkedHashMap<>();
    }

    public void startTrace(String inputPid, String inputPiid) {
        final String pid = inputPid == null ? DEFAULT_PID : inputPid;
        final String piid = inputPiid == null ? DEFAULT_PIID : inputPiid;
        LOGGER.info(String.format("Trace %s in log %s started.", piid, pid));

        this.findOrCreateTrace(pid, piid);
    }

    private void findOrCreateTrace(String pid, String piid) {
        if (this.traces.containsKey(pid) && this.traces.get(pid).containsKey(piid)) {
            this.element = this.traces.get(pid).get(piid);
            return;
        }

        final XTrace trace = new XTraceImpl(new XAttributeMapImpl());
        this.element = trace;
        this.addStringValue(PIID_ATTRIBUTE, piid);

        this.traces.putIfAbsent(pid, new LinkedHashMap<>());
        this.traces.get(pid).put(piid, trace);
    }

    public static void startEvent(XesWriter writer, String inputPid, String inputPiid, String inputEid) {
        final String pid = inputPid == null ? DEFAULT_PID : inputPid;
        final String piid = inputPiid == null ? DEFAULT_PIID : inputPiid;
        final String eid = inputEid == null ? String.format("%s%s", DEFAULT_EID, Long.toString(XesWriter.eid++)) : inputEid;
        LOGGER.info(String.format("Event %s in trace %s in log %s started.", eid, piid, pid));

        writer.findOrCreateTrace(pid, piid);
        writer.findOrCreateEvent(pid, piid, eid);
    }

    private void findOrCreateEvent(String pid, String piid, String eid) {
        if (this.events.containsKey(pid) && this.events.get(pid).containsKey(piid) && this.events.get(pid).get(piid).containsKey(eid)) {
            this.element = this.events.get(pid).get(piid).get(eid);
            return;
        }

        final XEvent event = new XEventImpl(new XAttributeMapImpl());
        this.element = event;
        this.addStringValue(EID_ATTRIBUTE, eid);

        this.events.putIfAbsent(pid, new LinkedHashMap<>());
        this.events.get(pid).putIfAbsent(piid, new LinkedHashMap<>());
        this.events.get(pid).get(piid).put(eid, event);
    }

    public void addBooleanValue(@NonNull String key, boolean value) {
        this.addAttribute(key, value, XAttributeBooleanImpl::new);
        LOGGER.info(String.format("Boolean attribute %s added.", key));
    }

    public void addBooleanList(@NonNull String key, @NonNull List<Boolean> values) {
        this.addListAttribute(key, values, XAttributeBooleanImpl::new);
        LOGGER.info(String.format("Boolean list attribute %s added.", key));
    }

    public void addFloatValue(@NonNull String key, @NonNull BigInteger value) {
        this.addAttribute(key, value.doubleValue(), XAttributeContinuousImpl::new);
        LOGGER.info(String.format("Float attribute %s added.", key));
    }

    public void addFloatList(@NonNull String key, @NonNull List<BigInteger> values) {
        final List<Double> list = values.stream().map(BigInteger::doubleValue).collect(Collectors.toList());
        this.addListAttribute(key, list, XAttributeContinuousImpl::new);
        LOGGER.info(String.format("Float list attribute %s added.", key));
    }

    public void addIntValue(@NonNull String key, @NonNull BigInteger value) {
        this.addAttribute(key, value.longValue(), XAttributeDiscreteImpl::new);
        LOGGER.info(String.format("Int attribute %s added.", key));
    }

    public void addIntList(@NonNull String key, @NonNull List<BigInteger> values) {
        final List<Long> list = values.stream().map(BigInteger::longValue).collect(Collectors.toList());
        this.addListAttribute(key, list, XAttributeContinuousImpl::new);
        LOGGER.info(String.format("Int list attribute %s added.", key));
    }

    public void addDateValue(@NonNull String key, @NonNull BigInteger value) {
        final Date date = new Date(value.longValue());
        this.addAttribute(key, date, XAttributeTimestampImpl::new);
        LOGGER.info(String.format("Date attribute %s added.", key));
    }

    public void addDateList(@NonNull String key, @NonNull List<BigInteger> values) {
        final List<Date> list = values.stream().map(BigInteger::longValue).map(Date::new).collect(Collectors.toList());
        this.addListAttribute(key, list, XAttributeTimestampImpl::new);
        LOGGER.info(String.format("Date list attribute %s added.", key));
    }

    public void addStringValue(@NonNull String key, @NonNull String value) {
        this.addAttribute(key, value, XAttributeLiteralImpl::new);
        LOGGER.info(String.format("String attribute %s = %s added.", key, value));
    }

    public void addStringList(@NonNull String key, @NonNull List<String> values) {
        this.addListAttribute(key, values, XAttributeLiteralImpl::new);
        LOGGER.info(String.format("String list attribute %s added.", key));
    }

    private <T> void addListAttribute(String key, List<T> list, BiFunction<String, T, XAttribute> attributeCreator) {
        final XAttributeListImpl attrList = new XAttributeListImpl(key);
        for (int i = 0; i < list.size(); i++) {
            T value = list.get(i);
            XAttribute attr = attributeCreator.apply(String.format("%s%s", key, i), value);
            attrList.addToCollection(attr);
        }
        this.element.getAttributes().put(key, attrList);
    }

    private <T> void addAttribute(String key, T value, BiFunction<String, T, XAttribute> attributeCreator) {
        final XAttribute attr = attributeCreator.apply(key, value);
        this.element.getAttributes().put(key, attr);
    }

    @Override
    protected void writeState(String filenameSuffix) throws Throwable {
        LOGGER.info("Xes export started.");
        final Map<String, XLog> logs = this.getLogs();
        try {
            final File folder = this.getOutputFolder().toAbsolutePath().toFile();
            final XesXmlSerializer serializer = new XesXmlSerializer();
            for (Entry<String, XLog> entry : logs.entrySet()) {
                final String filename = String.format("log_%s_%s.xes", entry.getKey(), filenameSuffix);
                final File file = new File(folder, filename);
                try (final FileOutputStream outputStream = new FileOutputStream(file);) {
                    serializer.serialize(entry.getValue(), outputStream);
                }
            }
        } catch (Exception t) {
            LOGGER.info("Xes export finished unsuccessfully.");
            final String message = "Error exporting data to XES.";
            throw new ProgramException(message, t);
        } finally {
            this.events.clear();
            this.traces.clear();
            this.element = null;
        }
        LOGGER.info("Xes export finished.");
    }

    private Map<String, XLog> getLogs() {
        return Stream.concat(this.traces.keySet().stream(), this.events.keySet().stream())
            .distinct()
            .collect(Collectors.toMap(pid -> pid, this::createLog));
    }

    private XLog createLog(String pid) {
        final XLog log = new XLogImpl(new XAttributeMapImpl());
        this.element = log;
        this.addStringValue(PID_ATTRIBUTE, pid);
        this.addTracesToLog(log, pid);
        return log;
    }

    private void addTracesToLog(XLog log, String pid) {
        this.traces.getOrDefault(pid, new LinkedHashMap<>()).entrySet().stream().forEach(entry -> {
            log.add(entry.getValue());
            addEventsToLog(entry.getValue(), pid, entry.getKey());
        });
    }

    private void addEventsToLog(XTrace trace, String pid, String piid) {
        this.events.getOrDefault(pid, new LinkedHashMap<>())
            .getOrDefault(piid, new LinkedHashMap<>())
            .entrySet()
            .stream()
            .forEach(entry -> trace.add(entry.getValue()));
    }

    public static final String BOOLEAN_TYPE = "xs:boolean";
    public static final String DATE_TYPE = "xs:date";
    public static final String FLOAT_TYPE = "xs:float";
    public static final String INT_TYPE = "xs:int";
    public static final String STRING_TYPE = "xs:string";
    private static final Map<String, Set<String>> SUPPORTED_SOL_TO_XES_CASTS = Map.of(
        TypeUtils.ADDRESS_TYPE_KEYWORD,
        Set.of(STRING_TYPE),
        TypeUtils.BYTES_TYPE_KEYWORD,
        Set.of(STRING_TYPE),
        TypeUtils.BOOL_TYPE_KEYWORD,
        Set.of(BOOLEAN_TYPE, STRING_TYPE),
        TypeUtils.INT_TYPE_KEYWORD,
        Set.of(STRING_TYPE, FLOAT_TYPE, INT_TYPE, DATE_TYPE),
        TypeUtils.STRING_TYPE_KEYWORD,
        Set.of(STRING_TYPE)
    );

    public static boolean areTypesCompatible(String solType, String xesType) {
        final String rootType = TypeUtils.getRootType(solType);
        return SUPPORTED_SOL_TO_XES_CASTS.getOrDefault(rootType, Collections.emptySet()).contains(xesType);
    }
}