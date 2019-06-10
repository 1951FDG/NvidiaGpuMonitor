package ua.pp.msk.openvpnstatus.implementation;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import ua.pp.msk.openvpnstatus.api.Client;
import ua.pp.msk.openvpnstatus.api.Route;
import ua.pp.msk.openvpnstatus.api.Status;
import ua.pp.msk.openvpnstatus.exceptions.OpenVpnParseException;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class OpenVpnStatus extends OpenVpnCommand implements Status {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenVpnStatus.class);

    private final List<Client> clientList = new ArrayList<>(10);

    private final Set<Route> routeSet = new HashSet<>(10);

    private Calendar updatedAt;

    @NotNull
    @Override
    public String toString() {
        DateFormat df = DateFormat.getInstance();
        StringBuilder sb = new StringBuilder("Updated:\t");
        sb.append(df.format(updatedAt.getTime()));
        sb.append(System.lineSeparator());
        sb.append("Client List:");
        sb.append(System.lineSeparator());
        for (Client client : clientList) {
            sb.append('\t');
            sb.append(client);
            sb.append(System.lineSeparator());
        }
        sb.append("Routes list:");
        sb.append(System.lineSeparator());
        for (Route route : routeSet) {
            sb.append('\t');
            sb.append(route);
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public List<Client> getClientList() {
        return Collections.unmodifiableList(clientList);
    }

    @NotNull
    @Override
    public Set<Route> getRoutes() {
        return Collections.unmodifiableSet(routeSet);
    }

    @Override
    public Calendar getUpdateTime() {
        return (Calendar) updatedAt.clone();
    }

    @Override
    public void setCommandOutput(String output) throws OpenVpnParseException {
        LOGGER.info("Parsing: {}{}", System.lineSeparator(), output);
        super.setCommandOutput(output);
    }

    @SuppressWarnings({ "AssignmentToForLoopParameter", "ValueOfIncrementOrDecrementUsed", "MethodCallInLoopCondition", "ChainedMethodCall",
            "OverlyComplexMethod", "OverlyLongMethod" })
    @Override
    public void setCommandOutput(String[] lines) throws OpenVpnParseException {
        Pattern statsHeader = Pattern.compile("^OpenVPN STATISTICS");
        Pattern clientsHeader = Pattern.compile("^OpenVPN CLIENT LIST");
        Pattern updated = Pattern.compile("^Updated,.*");
        Pattern clientColumns = Pattern.compile("Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since");
        Pattern routesHeader = Pattern.compile("^ROUTING TABLE");
        Pattern routesColumns = Pattern.compile("Virtual Address,Common Name,Real Address,Last Ref");
        Pattern globalStats = Pattern.compile("GLOBAL STATS");
        String msg = "Cannot parse OpenVPN status. Wrong lines sequence.";
        int length = lines.length;
        for (int i = 0; i < length; i++) {
            if (statsHeader.matcher(lines[i]).matches()) {
                if (updated.matcher(lines[++i]).matches()) {
                    try {
                        updatedAt = parseUpdatedTime(lines[i++]);
                    } catch (OpenVpnParseException ex) {
                        LOGGER.error("Cannot parse update date", ex);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
            if (clientsHeader.matcher(lines[i]).matches()) {
                if (updated.matcher(lines[++i]).matches()) {
                    try {
                        updatedAt = parseUpdatedTime(lines[i++]);
                    } catch (OpenVpnParseException ex) {
                        LOGGER.error("Cannot parse update date", ex);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
                if (clientColumns.matcher(lines[i++]).matches()) {
                    while (!routesHeader.matcher(lines[i]).matches()) {
                        addClient(lines[i++]);
                    }
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
            if (routesHeader.matcher(lines[i]).matches()) {
                i++;
                if (routesColumns.matcher(lines[i]).matches()) {
                    i++;
                    while (!globalStats.matcher(lines[i]).matches()) {
                        addRoute(lines[i++]);
                    }
                    break;
                } else {
                    throw new OpenVpnParseException(msg);
                }
            }
        }
        LOGGER.info("Successfully parsed {}{}", System.lineSeparator(), this);
    }

    private void addClient(String clientString) {
        try {
            Client ovc = new OpenVpnClient(clientString);
            clientList.add(ovc);
        } catch (OpenVpnParseException ex) {
            LOGGER.error("Cannot add the client", ex);
        }
    }

    private void addRoute(String routeString) {
        try {
            Route ovr = new OpenVpnRoute(routeString);
            routeSet.add(ovr);
        } catch (OpenVpnParseException ex) {
            LOGGER.error("Cannot add route", ex);
        }
    }

    private static Calendar parseUpdatedTime(String updatedString) throws OpenVpnParseException {
        Calendar ut;
        try {
            String[] components = updatedString.split(",");
            if (components.length != 2) {
                throw new OpenVpnParseException("Cannot parse update time string. There should be 2 components separated by comma");
            }
            SimpleDateFormat sdf = new SimpleDateFormat(Status.DATE_FORMAT, Locale.ROOT);
            Date parsedDate = sdf.parse(components[1]);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            ut = calendar;
        } catch (ParseException ex) {
            String msg = "Cannot parse update time string";
            LOGGER.info(msg, ex);
            throw new OpenVpnParseException(msg, ex);
        }
        return ut;
    }
}
