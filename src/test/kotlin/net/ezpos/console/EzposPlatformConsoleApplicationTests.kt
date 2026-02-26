package net.ezpos.console

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [EzposPlatformConsoleApplication::class])
@Disabled("Requires running PostgreSQL and Redis — enable for integration testing")
class EzposPlatformConsoleApplicationTests {

	@Test
	fun contextLoads() {
	}

}
