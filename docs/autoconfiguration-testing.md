# Testing Spring Boot Auto-Configuration

Effective auto-configuration tests combine unit, slice, and integration styles to cover the bean registration flow from multiple angles.

## 1. Validate Conditions in Focused Tests
Use `ApplicationContextRunner` (Gradle dependency `spring-boot-test`) to instantiate the auto-configuration with minimal context. Assert that conditional beans load when expected and remain absent otherwise. Prefer targeted property overrides per scenario to keep the test small.

```kotlin
private val contextRunner = ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(MyAutoConfiguration::class.java))

@Test
fun `creates client when token present`() {
    contextRunner
        .withPropertyValues("telegram.bot.token=secret")
        .run { assertThat(it).hasSingleBean(TelegramClient::class.java) }
}
```

## 2. Cover Negative Paths Explicitly
Auto-configuration failures often stem from unmet conditions. Add dedicated tests for missing properties, incompatible classpath combinations, or customizers that disable beans. Ensure the `ConditionEvaluationReport` contains the expected outcomes when the bean is absent.

## 3. Exercise Customization Hooks
If the auto-configuration exposes customizers (e.g., `TelegramClientCustomizer` beans), wire sample beans into the `ApplicationContextRunner` and assert that they are applied in bean post-processing.

## 4. Integrate with Spring Boot Test Slices
When auto-configuration integrates with web, data, or messaging layers, combine it with relevant test slices (`@WebMvcTest`, `@DataJpaTest`) to ensure compatibility with Boot's managed infrastructure.

## 5. Provide End-to-End Assurance
Keep at least one full `@SpringBootTest` that uses the starter the way downstream users would, ideally in a sample application module. Validate critical beans and interactions, and consider running lightweight smoke tests (e.g., hitting an exposed HTTP endpoint) to catch wiring regressions.

## 6. Use AssertJ and AutoConfigure Assertions
Rely on Spring Boot's AssertJ extensions (`ConditionOutcomeMatcher`) or custom assert helpers to express bean presence, property binding, and condition matches clearly. Prefer expressive assertions over manual context lookups.

## 7. Guard Against Regression with ContextCaching
Leverage JUnit 5's `@DirtiesContext` sparingly; design tests to reuse cached contexts for speed. Group related scenarios in nested test classes to minimize context builds while keeping readability.

Following this layered strategy ensures that the module's auto-configuration remains reliable as Spring Boot evolves and consumers integrate it into diverse applications.
