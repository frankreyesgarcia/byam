/*
 * Copyright 2015 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.resourcemanager;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.cloud.Policy;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A Google Cloud Resource Manager project object.
 *
 * <p>A Project is a high-level Google Cloud Platform entity. It is a container for ACLs, APIs,
 * AppEngine Apps, VMs, and other Google Cloud Platform resources. This class' member variables are
 * immutable. Methods that change or update the underlying Project information return a new Project
 * instance. {@code Project} adds a layer of service-related functionality over {@link ProjectInfo}.
 *
 * @deprecated v3 GAPIC client of ResourceManager is now available
 */
@Deprecated
public class Project extends ProjectInfo {

  private static final long serialVersionUID = 6767630161335155133L;

  private final ResourceManagerOptions options;
  private transient ResourceManager resourceManager;

  /** Builder for {@code Project}. */
  public static class Builder extends ProjectInfo.Builder {
    private final ResourceManager resourceManager;
    private final ProjectInfo.BuilderImpl infoBuilder;

    Builder(Project project) {
      this.resourceManager = project.resourceManager;
      this.infoBuilder = new ProjectInfo.BuilderImpl(project);
    }

    @Override
    public Builder setName(String name) {
      infoBuilder.setName(name);
      return this;
    }

    @Override
    public Builder setProjectId(String projectId) {
      infoBuilder.setProjectId(projectId);
      return this;
    }

    @Override
    public Builder addLabel(String key, String value) {
      infoBuilder.addLabel(key, value);
      return this;
    }

    @Override
    public Builder removeLabel(String key) {
      infoBuilder.removeLabel(key);
      return this;
    }

    @Override
    public Builder clearLabels() {
      infoBuilder.clearLabels();
      return this;
    }

    @Override
    public Builder setLabels(Map<String, String> labels) {
      infoBuilder.setLabels(labels);
      return this;
    }

    @Override
    Builder setProjectNumber(Long projectNumber) {
      infoBuilder.setProjectNumber(projectNumber);
      return this;
    }

    @Override
    Builder setState(State state) {
      infoBuilder.setState(state);
      return this;
    }

    @Override
    Builder setCreateTimeMillis(Long createTimeMillis) {
      infoBuilder.setCreateTimeMillis(createTimeMillis);
      return this;
    }

    @Override
    public Builder setParent(ResourceId parent) {
      infoBuilder.setParent(parent);
      return this;
    }

    @Override
    public Project build() {
      return new Project(resourceManager, infoBuilder);
    }
  }

  Project(ResourceManager resourceManager, ProjectInfo.BuilderImpl infoBuilder) {
    super(infoBuilder);
    this.resourceManager = checkNotNull(resourceManager);
    this.options = resourceManager.getOptions();
  }

  /** Returns the {@link ResourceManager} service object associated with this Project. */
  public ResourceManager getResourceManager() {
    return resourceManager;
  }

  /**
   * Fetches the project's latest information. Returns {@code null} if the project does not exist.
   *
   * @return Project containing the project's updated metadata or {@code null} if not found
   * @throws ResourceManagerException upon failure
   */
  public Project reload() {
    return resourceManager.get(getProjectId());
  }

  /**
   * Marks the project identified by the specified project ID for deletion.
   *
   * @throws ResourceManagerException upon failure
   */
  public void delete() {
    resourceManager.delete(getProjectId());
  }

  /**
   * Restores the project identified by the specified project ID.
   *
   * @throws ResourceManagerException upon failure (including when the project can't be restored)
   */
  public void undelete() {
    resourceManager.undelete(getProjectId());
  }

  /**
   * Replaces the attributes of the project with the attributes of this project.
   *
   * @return the Project representing the new project metadata
   * @throws ResourceManagerException upon failure
   */
  public Project replace() {
    return resourceManager.replace(this);
  }

  /**
   * Returns the IAM access control policy for this project. Returns {@code null} if the resource
   * does not exist or if you do not have adequate permission to view the project or get the policy.
   *
   * @return the IAM policy for the project
   * @throws ResourceManagerException upon failure
   */
  public Policy getPolicy() {
    return resourceManager.getPolicy(getProjectId());
  }

  /**
   * Sets the IAM access control policy for this project. Replaces any existing policy. It is
   * recommended that you use the read-modify-write pattern.
   *
   * @return the newly set IAM policy for this project
   * @throws ResourceManagerException upon failure
   */
  public Policy replacePolicy(Policy newPolicy) {
    return resourceManager.replacePolicy(getProjectId(), newPolicy);
  }

  /**
   * Returns the permissions that a caller has on this project.
   *
   * @return a list of booleans representing whether the caller has the permissions specified
   * @throws ResourceManagerException upon failure
   */
  List<Boolean> testPermissions(List<String> permissions) {
    return resourceManager.testPermissions(getProjectId(), permissions);
  }

  @Override
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !obj.getClass().equals(Project.class)) {
      return false;
    }
    Project other = (Project) obj;
    return Objects.equals(toPb(), other.toPb()) && Objects.equals(options, other.options);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(super.hashCode(), options);
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.resourceManager = options.getService();
  }

  static Project fromPb(
      ResourceManager resourceManager,
      com.google.cloud.resourcemanager.v3.Project answer) {
    ProjectInfo info = ProjectInfo.fromPb(answer);
    return new Project(resourceManager, new ProjectInfo.BuilderImpl(info));
  }
}