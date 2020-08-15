package de.skuzzle.cmp.counter.tallypage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.mobile.device.Device;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.google.common.base.Preconditions;

import de.skuzzle.cmp.auth.TallyUser;
import de.skuzzle.cmp.counter.client.BackendClient;
import de.skuzzle.cmp.counter.client.Filter;
import de.skuzzle.cmp.counter.client.RestIncrements;
import de.skuzzle.cmp.counter.client.RestShareInformation;
import de.skuzzle.cmp.counter.client.RestTallyIncrement;
import de.skuzzle.cmp.counter.client.RestTallyResponse;
import de.skuzzle.cmp.counter.client.RestTallySheet;
import de.skuzzle.cmp.counter.client.Tags;
import de.skuzzle.cmp.counter.graphs.Graph;
import de.skuzzle.cmp.counter.tagcloud.TagCloud;
import de.skuzzle.cmp.counter.timeline.Timeline;
import de.skuzzle.cmp.counter.timeline.TimelineBuilder;
import de.skuzzle.cmp.counter.urls.KnownUrls;
import de.skuzzle.cmp.ui.socialcard.SocialCard;

@Controller
public class TallyPageController {

    private final BackendClient client;
    private final TallyUser currentUser;

    public TallyPageController(BackendClient client, TallyUser currentUser) {
        this.client = client;
        this.currentUser = currentUser;
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ModelAndView handleClientError(HttpStatusCodeException e) {
        final ModelAndView modelAndView = new ModelAndView();
        final HttpStatus statusCode = e.getStatusCode();
        if (statusCode == HttpStatus.NOT_FOUND) {
            modelAndView.setViewName("error/404");
        } else {
            modelAndView.setViewName("error");
        }
        modelAndView.addObject("statusCode", statusCode);
        modelAndView.setStatus(statusCode);
        return modelAndView;
    }

    @ModelAttribute("user")
    public TallyUser getUser() {
        return currentUser;
    }

    @GetMapping(KnownUrls.VIEW_COUNTER_STRING)
    public ModelAndView showTallySheet(
            @PathVariable String key,
            @RequestParam(defaultValue = "") Set<String> tags,
            Device device) {

        final Tags filterTags = Tags.fromCollection(tags);
        final Filter currentFilter = Filter.all().withTags(filterTags);

        final RestTallyResponse response = client.getTallySheet(key, currentFilter);

        final RestTallySheet tallySheet = response.getTallySheet();
        final RestIncrements increments = response.getIncrements();

        final Graph graph = Graph.fromHistory(increments.getEntries());
        final Timeline timeline = TimelineBuilder.fromBackendResponse(response);
        final TagCloud tagCloud = TagCloud.fromBackendResponse(key, response, currentFilter);
        final SocialCard socialCard = SocialCard.withTitle(tallySheet.getName())
                .withDescription("Current count: " + tallySheet.getTotalCount())
                .build();
        final CounterMetaInfo counterMetaInfo = CounterMetaInfo.fromBackendResponse(response);
        final List<Share> shares = Share.fromBackendResponse(tallySheet);

        final boolean mobile = !device.isNormal();
        return new ModelAndView("tallypage/tally", Map.of(
                "currentFilter", currentFilter,
                "key", key,
                "tagCloud", tagCloud,
                "counterMetaInfo", counterMetaInfo,
                "timeline", timeline,
                "increments", increments,
                "graph", graph,
                "mobile", mobile,
                "socialCard", socialCard,
                "shares", shares));
    }

    @PostMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = "action=increment")
    public View incrementTallySheet(
            @PathVariable String key,
            @RequestParam String description,
            @RequestParam String tags,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate incrementDateUTC) {

        final Set<String> tagSet = Tags.fromString(tags).all();
        final RestTallyIncrement increment = RestTallyIncrement.createNew(description,
                LocalDateTime.of(incrementDateUTC, LocalTime.now()), tagSet);

        client.increment(key, increment);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = "action=addTag")
    public View addFilterTag(@PathVariable String key) {
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.INCREMENT_COUNTER_STRING, params = "action=updateIncrement")
    public View updateIncrement(
            @PathVariable String key,
            @PathVariable String incrementId,
            @RequestParam String description,
            @RequestParam String tags,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate incrementDateUTC) {

        final Set<String> tagSet = Tags.fromString(tags).all();
        final RestTallyIncrement increment = RestTallyIncrement.createWithId(incrementId, description,
                LocalDateTime.of(incrementDateUTC, LocalTime.now()), tagSet);
        client.updateIncrement(key, increment);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = "action=assignToCurrentUser")
    public View assignToCurrentUser(@PathVariable String key) {
        Preconditions.checkState(this.currentUser.isLoggedIn(), "Can't assign to current user: user not logged in");
        client.assignToCurrentUser(key);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.INCREMENT_COUNTER_STRING, params = "action=delete")
    public View deleteIncrement(@PathVariable String key, @PathVariable String incrementId) {
        client.deleteIncrement(key, incrementId);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = { "action=changeName", "newName" })
    public View changeTitle(@PathVariable String key, @RequestParam String newName) {
        client.changeName(key, newName);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @PostMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = { "action=share" })
    public View addShare(@PathVariable String key,
            @RequestParam(name = "showIncrements", defaultValue = "false") boolean showIncrements,
            @RequestParam(name = "showIncrementTags", defaultValue = "false") boolean showIncrementTags,
            @RequestParam(name = "showIncrementDescription", defaultValue = "false") boolean showIncrementDescription) {

        client.addShare(key, RestShareInformation.create(showIncrements, showIncrementTags, showIncrementDescription));
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }

    @GetMapping(path = KnownUrls.VIEW_COUNTER_STRING, params = { "action=deleteShare", "shareId" })
    public View deleteShare(@PathVariable String key,
            @RequestParam("shareId") String shareId) {

        client.deleteShare(key, shareId);
        return KnownUrls.VIEW_COUNTER.turboRedirectResolve(key);
    }
}
