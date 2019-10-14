package de.skuzzle.tally.rest;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.skuzzle.tally.rest.auth.TallyUser;
import de.skuzzle.tally.rest.ratelimit.ApiRateLimiter;
import de.skuzzle.tally.rest.ratelimit.RateLimitExceededException;
import de.skuzzle.tally.service.IncrementNotAvailableException;
import de.skuzzle.tally.service.IncrementQuery;
import de.skuzzle.tally.service.IncrementQueryResult;
import de.skuzzle.tally.service.ShallowTallySheet;
import de.skuzzle.tally.service.TallyService;
import de.skuzzle.tally.service.TallySheet;
import de.skuzzle.tally.service.TallySheetNotAvailableException;
import de.skuzzle.tally.service.UserAssignmentException;
import de.skuzzle.tally.service.UserId;

@RestController
public class TallyRestController {

    private final TallyService tallyService;
    private final ApiRateLimiter<HttpServletRequest> rateLimiter;

    TallyRestController(TallyService tallyService, ApiRateLimiter<HttpServletRequest> rateLimiter) {
        this.tallyService = tallyService;
        this.rateLimiter = rateLimiter;
    }

    private UserId currentUser() {
        final TallyUser requestUser = TallyUser.fromCurrentAuthentication();
        return UserId.of(requestUser.getSource(), requestUser.getId(), requestUser.isAnonymous());
    }

    @GetMapping("/")
    public ResponseEntity<RestTallySheetsReponse> getAllTallies(HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(request);
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
            HttpServletRequest request) {

        rateLimiter.blockIfRateLimitIsExceeded(request);
        final TallySheet tallySheet = tallyService.getTallySheet(key);

        final IncrementQueryResult incrementQueryResult = tallySheet.selectIncrements(IncrementQuery.all()
                .from(from == null ? LocalDateTime.MIN : from.atStartOfDay())
                .until(until == null ? LocalDateTime.MAX : until.atStartOfDay())
                .start(start < 0 ? 0 : start)
                .maxResults(max < 0 ? Integer.MAX_VALUE : max));

        final UserId currentUser = currentUser();
        final RestIncrements increments = RestIncrements.of(incrementQueryResult);
        final RestTallySheet restTallySheet = RestTallySheet.fromDomainObject(currentUser, tallySheet);
        final RestTallyResponse response = RestTallyResponse.of(restTallySheet, increments);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{name}")
    public ResponseEntity<RestTallyResponse> createTally(@PathVariable @NotEmpty String name,
            HttpServletRequest request) {

        rateLimiter.blockIfRateLimitIsExceeded(request);
        final UserId currentUser = currentUser();
        final TallySheet tallySheet = tallyService.createNewTallySheet(currentUser, name);

        final RestIncrements increments = RestIncrements.empty(0);
        final RestTallySheet restTallySheet = RestTallySheet.fromDomainObject(currentUser, tallySheet);
        final RestTallyResponse response = RestTallyResponse.of(restTallySheet, increments);

        return ResponseEntity
                .created(URI.create("/" + tallySheet.getAdminKey().orElseThrow()))
                .body(response);
    }

    @PostMapping("/{key}/assignToCurrentUser")
    @ResponseStatus(HttpStatus.OK)
    public void assignToCurrentUser(@PathVariable @NotEmpty String key,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(request);
        final UserId currentUser = currentUser();
        tallyService.assignToUser(key, currentUser);

    }

    @PostMapping("/{key}/increment")
    @ResponseStatus(HttpStatus.OK)
    public void increment(@PathVariable String key,
            @RequestBody @Valid RestTallyIncrement increment,
            HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(request);
        tallyService.increment(key, increment.toDomainObjectWithoutId());
    }

    @DeleteMapping("/{key}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteTallySheet(@PathVariable String key, HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(request);
        tallyService.deleteTallySheet(key);
    }

    @DeleteMapping("/{key}/increment/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteIncrement(@PathVariable String key, @PathVariable String id, HttpServletRequest request) {
        rateLimiter.blockIfRateLimitIsExceeded(request);
        tallyService.deleteIncrement(key, id);
    }

    @ExceptionHandler(UserAssignmentException.class)
    public ResponseEntity<RestErrorMessage> onUserAssignmentFailed(UserAssignmentException e) {
        final RestErrorMessage body = RestErrorMessage.of(e.getMessage(), e.getClass().getName());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<RestErrorMessage> onRateLimitExceeded(RateLimitExceededException e) {
        final RestErrorMessage body = RestErrorMessage.of(e.getMessage(), e.getClass().getName());
        return new ResponseEntity<>(body, HttpStatus.BANDWIDTH_LIMIT_EXCEEDED);
    }

    @ExceptionHandler(value = { TallySheetNotAvailableException.class, IncrementNotAvailableException.class })
    public ResponseEntity<RestErrorMessage> onTallySheetNotAvailable(Exception e) {
        final RestErrorMessage body = RestErrorMessage.of(e.getMessage(), e.getClass().getName());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

}
