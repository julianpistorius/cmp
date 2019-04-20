package contracts.tally

import org.springframework.cloud.contract.spec.internal.RegexProperty

import java.util.regex.Pattern

class Helpers {
    protected static final Pattern ISO_DATE_TIME_WITH_NANOS = Pattern.
            compile('([0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])\\.\\d+')

    static RegexProperty isoDateTimeWithNanos() {
        return new RegexProperty(ISO_DATE_TIME_WITH_NANOS).asString()
    }
}

org.springframework.cloud.contract.spec.Contract.make {
    name("incrementExistingTallySheet")
    request {
        method POST()
        url '/admin/adminKey'
        body([
                incrementDateUTC: $(
                        consumer(regex(Helpers.isoDateTimeWithNanos())),
                        producer("2019-04-12T11:21:32.123")),
                description: "Description",
                tags: [ 'tag1', 'tag2']
        ])
        headers {
            contentType(applicationJson())
        }
    }
    response {
        status OK()
        body([
            id: $(
                consumer('5c9dc2ce8691ff4f8c1b2d54'),
                producer(regex('[a-zA-Z0-9]+'))
            ),
            name: 'existing',
            publicKey: 'publicKey',
            adminKey: 'adminKey',
            createDateUTC: $(
                consumer('1987-09-12T11:11:00.123'),
                producer(regex(Helpers.isoDateTimeWithNanos()))
            ),
            lastModifiedDateUTC: $(
                consumer('1987-09-12T11:11:00.123'),
                producer(regex(Helpers.isoDateTimeWithNanos()))
            ),
            increments: [
                [
                    id: regex(uuid()),
                    description: regex('\\w+'),
                    tags: [ 'tag1', 'tag2' ],
                    createDateUTC: $(
                        consumer('1987-09-12T11:11:00.123'),
                        producer(regex(Helpers.isoDateTimeWithNanos()))
                    ),
                    incrementDateUTC: $(
                            consumer('2019-04-12T11:21:32.123'),
                            producer(regex(Helpers.isoDateTimeWithNanos()))
                    )
                ]
            ]
        ])
        headers {
            contentType(applicationJson())
        }
    }
}
