<repair_strategy>
1. The class `PublishMetadata` has been removed in the new version of the dependency.
2. The method `newPublisher` uses `PublishMetadata` as a type parameter, which is no longer available.
3. The client code needs to be updated to use a different type parameter that is compatible with the new version of the dependency.
4. The function signature of `newPublisher` cannot be changed.
5. The minimal change would be to replace `PublishMetadata` with a compatible type from the new dependency version.
6. The change should not affect other parts of the code.
7. Ensure the class is compilable.
8. No additional imports are needed if a compatible type is found within the same package.
</repair_strategy>

