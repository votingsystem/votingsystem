package org.votingsystem.web.accesscontrol.jaxrs;

import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import org.votingsystem.model.voting.EventElection;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

@Path("/subscription")
public class SubscriptionResource {

    private static final Logger log = Logger.getLogger(SubscriptionResource.class.getName());

    @Inject DAOBean dao;
    @Inject ConfigVS config;
    private MessagesVS messages = MessagesVS.getCurrentInstance();

    List<String> supportedFormats = Arrays.asList("rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93",
            "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3");

    @Path("/elections")
    @GET @Produces(MediaType.APPLICATION_JSON)
    public Response elections(@DefaultValue("rss_2.0") @QueryParam("feedType") String feedType) throws Exception {
        return Response.ok().entity(getElectionFeeds(feedType)).type(MediaType.APPLICATION_XML).build();
    }

    private String getElectionFeeds(String feedType) throws IOException, FeedException {
        Query query = dao.getEM().createQuery("select e from EventElection e where e.state =:state order by e.dateCreated DESC")
                .setParameter("state", EventVS.State.ACTIVE);
        List<EventElection> eventVSList = query.getResultList();
        List<SyndEntryImpl> feeds = new ArrayList<>();
        for(EventElection eventElection : eventVSList) {
            String electionURL = config.getContextURL() + "/rest/eventElection/id/" + eventElection.getId();
            SyndEntryImpl entry = new SyndEntryImpl();
            entry.setTitle(eventElection.getSubject());
            entry.setLink(electionURL);
            entry.setPublishedDate(eventElection.getDateCreated());
            SyndContent description = new SyndContentImpl();
            description.setType("text/plain");
            String feedContent = format("<p>{0}</p><a href=''{1}''>{2}</a>",
                    new String(Base64.getDecoder().decode(eventElection.getContent().getBytes())),
                    electionURL, messages.get("voteLbl"));
            description.setValue(feedContent);
            entry.setDescription(description);
            feeds.add(entry);
        }
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType(feedType);
        feed.setTitle(messages.get("electionsSubscriptionTitle") + " - " + config.getServerName());
        feed.setLink(config.getContextURL() + "/rest/eventElection");
        feed.setDescription(messages.get("electionsSubscriptionDescription"));
        feed.setEntries(feeds);

        StringWriter writer = new StringWriter();
        SyndFeedOutput output = new SyndFeedOutput();
        output.output(feed, writer);
        writer.close();
        return writer.toString();
    }

}
