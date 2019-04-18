package de.skuzzle.tally.frontend;

import com.google.common.collect.ImmutableMap;
import de.skuzzle.tally.frontend.client.TallyClient;
import de.skuzzle.tally.frontend.client.TallyIncrement;
import de.skuzzle.tally.frontend.client.TallySheet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

@Controller
public class TallyController {

    private final TallyClient client;

    public TallyController(TallyClient client) {
        this.client = client;
    }

    @PostMapping("/_create")
    public String createTallySheet(@RequestParam("name") String name) {
        final TallySheet tallySheet = client.createNewTallySheet(name).orElseThrow();
        return "redirect:/" + tallySheet.getAdminKey();
    }

    @PostMapping("/{adminKey}")
    public String incrementTallySheet(@PathVariable("adminKey") String adminKey, @RequestParam("description") String description) {
        final TallyIncrement increment = new TallyIncrement();
        increment.setDescription(description);
        increment.setTags(new HashSet<>());
        final TallySheet tallySheet = client.increment(adminKey, increment).orElseThrow();
        return "redirect:/" + tallySheet.getAdminKey();
    }

    @GetMapping("/{key}")
    public ModelAndView showTallySheet(@PathVariable("key") String key) {
        final TallySheet tallySheet = client.getTallySheet(key).orElseThrow();

        return new ModelAndView("tally", ImmutableMap.of("tally", tallySheet));
    }
}
