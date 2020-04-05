package de.skuzzle.cmp.counter.domain;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.skuzzle.cmp.common.http.RequestId;
import de.skuzzle.cmp.common.ratelimit.ApiRateLimiter;
import de.skuzzle.cmp.common.ratelimit.RateLimitedOperations;
import de.skuzzle.cmp.rest.auth.TallyUser;

@RestController
public class TallyRestController {

    private final TallyService tallyService;
    private final TallyUser currentUser;
    private final ApiRateLimiter<HttpServletRequest> rateLimiter;

    TallyRestController(TallyService tallyService, TallyUser currentUser,
            ApiRateLimiter<HttpServletRequest> rateLimiter) {
        this.tallyService = tallyService;
        this.currentUser = currentUser;
        this.rateLimiter = rateLimiter;
    }

    private UserId currentUser() {
        return UserId.of(currentUser.getSource(), currentUser.getId(), currentUser.isAnonymous());
    }

    @GetMapping("/_meta")
    public ResponseEntity<RestTallyMetaInfoResponse> getMetaInfo(HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.FREE, request);

        final int countAllTallySheets = tallyService.countAllTallySheets();
        final RestTallyMetaInfoResponse body = RestTallyMetaInfoResponse.of(countAllTallySheets);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/")
    public ResponseEntity<RestTallySheetsReponse> getAllTallies(HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.EXPENSIVE, request);

        final UserId currentUser = currentUser();

        final List<ShallowTallySheet> tallySheets = tallyService.getTallySheetsFor(currentUser);
        final List<RestTallySheet> restTallySheets = RestTallySheet.fromDomainObjects(currentUser, tallySheets);
        final RestTallySheetsReponse response = RestTallySheetsReponse.of(restTallySheets);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{key}")
    public ResponseEntity<RestTallyResponse> getTally(@PathVariable String key,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate until,
            @RequestParam(required = false, defaultValue = "-1") int start,
            @RequestParam(required = false, defaultValue = "-1") int max,
            @RequestParam(required = false, defaultValue = "") Set<String> tags,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.CHEAP, request);

        final TallySheet tallySheet = tallyService.getTallySheet(key);

        final IncrementQueryResult incrementQueryResult = tallySheet.selectIncrements(IncrementQuery.all()
                .from(from == null ? LocalDateTime.MIN : from.atStartOfDay())
                .until(until == null ? LocalDateTime.MAX : until.atStartOfDay())
                .start(start < 0 ? 0 : start)
                .maxResults(max < 0 ? Integer.MAX_VALUE : max)
                .havingAllTags(tags));

        final UserId currentUser = currentUser();
        final RestIncrements increments = RestIncrements.of(incrementQueryResult);
        final RestTallySheet restTallySheet = RestTallySheet.fromDomainObject(currentUser, tallySheet);
        final RestTallyResponse response = RestTallyResponse.of(restTallySheet, increments);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{name}")
    public ResponseEntity<RestTallyResponse> createTally(@PathVariable @NotEmpty String name,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.VERY_EXPENSIVE, request);

        final UserId currentUser = currentUser();
        final TallySheet tallySheet = tallyService.createNewTallySheet(currentUser, name);

        final RestIncrements increments = RestIncrements.empty(0);
        final RestTallySheet restTallySheet = RestTallySheet.fromDomainObject(currentUser, tallySheet);
        final RestTallyResponse response = RestTallyResponse.of(restTallySheet, increments);

        return ResponseEntity
                .created(URI.create("/" + tallySheet.getAdminKey().orElseThrow()))
                .body(response);
    }

    @DeleteMapping("/{key}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteTallySheet(@PathVariable String key, HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.CHEAP, request);

        tallyService.deleteTallySheet(key);
    }

    @PostMapping("/{key}/share")
    @ResponseStatus(HttpStatus.CREATED)
    public void addShare(@PathVariable String key, @RequestBody RestShareInformation shareInformation,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.EXPENSIVE, request);
        tallyService.addShare(key, shareInformation.toDomainObject());
    }

    @DeleteMapping("/{key}/share/{shareId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteShare(@PathVariable String key, @PathVariable String shareId, HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.CHEAP, request);
        tallyService.deleteShare(key, shareId);
    }

    @PostMapping("/{key}/assignToCurrentUser")
    @ResponseStatus(HttpStatus.OK)
    public void assignToCurrentUser(@PathVariable @NotEmpty String key,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.CHEAP, request);

        final UserId currentUser = currentUser();
        tallyService.assignToUser(key, currentUser);

    }

    @PutMapping("/{key}/changeName/{newName}")
    @ResponseStatus(HttpStatus.OK)
    public void changeName(@PathVariable @NotEmpty String key, @PathVariable String newName,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.EXPENSIVE, request);

        tallyService.changeName(key, newName);
    }

    @PostMapping("/{key}/increment")
    @ResponseStatus(HttpStatus.OK)
    public void increment(@PathVariable String key,
            @RequestBody @Valid RestTallyIncrement increment,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.EXPENSIVE, request);

        tallyService.increment(key, increment.toDomainObjectWithoutId());
    }

    @PutMapping("/{key}/increment")
    @ResponseStatus(HttpStatus.OK)
    public void updateIncrement(@PathVariable String key, @RequestBody RestTallyIncrement increment,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.EXPENSIVE, request);

        tallyService.updateIncrement(key, increment.toDomainObjectWithId());
    }

    @DeleteMapping("/{key}/increment/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteIncrement(@PathVariable String key, @PathVariable String id, HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(RateLimitedOperations.CHEAP, request);

        tallyService.deleteIncrement(key, id);
    }

    @ExceptionHandler(UserAssignmentException.class)
    public ResponseEntity<RestErrorMessage> onUserAssignmentFailed(UserAssignmentException e) {
        final String requestId = RequestId.forCurrentThread();
        final RestErrorMessage body = RestErrorMessage.of(e.getMessage(), e.getClass().getSimpleName(), requestId);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = { TallySheetNotAvailableException.class, IncrementNotAvailableException.class,
            ShareNotAvailableException.class })
    public ResponseEntity<RestErrorMessage> onTallySheetNotAvailable(Exception e) {
        final String requestId = RequestId.forCurrentThread();
        final RestErrorMessage body = RestErrorMessage.of(e.getMessage(), e.getClass().getSimpleName(), requestId);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

}
