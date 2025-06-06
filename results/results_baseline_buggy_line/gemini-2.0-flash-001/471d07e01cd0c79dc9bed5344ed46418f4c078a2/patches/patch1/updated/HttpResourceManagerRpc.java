/*
 * Copyright 2016 Google LLC
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

package com.google.cloud.resourcemanager.spi.v1beta1;

import static com.google.cloud.RetryHelper.runWithRetries;
import static com.google.common.base.MoreObjects.firstNonNull;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.core.ApiClock;
import com.google.api.gax.retrying.ResultRetryAlgorithm;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.TimedAttemptSettings;
import com.google.cloud.resourcemanager.ResourceManagerException;
import com.google.cloud.resourcemanager.ResourceManagerOptions;
import com.google.cloud.resourcemanager.model.v3.GetEffectiveOrgPolicyRequest;
import com.google.cloud.resourcemanager.model.v3.GetOrgPolicyRequest;
import com.google.cloud.resourcemanager.model.v3.ListOrgPoliciesRequest;
import com.google.cloud.resourcemanager.model.v3.OrgPolicy;
import com.google.cloud.resourcemanager.v3.FoldersClient;
import com.google.cloud.resourcemanager.v3.OrganizationsClient;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.cloud.resourcemanager.v3.stub.ResourceManagerStubSettings;
import com.google.cloud.http.BaseHttpServiceException;
import com.google.cloud.http.HttpTransportOptions;
import com.google.cloud.Tuple;
import com.google.cloud.RetryHelper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.iam.v1.GetPolicyOptions;
import com.google.iam.v1.Policy;
import com.google.iam.v1.SetIamPolicyRequest;
import com.google.iam.v1.TestIamPermissionsRequest;
import com.google.iam.v1.TestIamPermissionsResponse;
import com.google.protobuf.Empty;
import com.google.rpc.Code;
import com.google.rpc.Status;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.threeten.bp.Duration;

/** @deprecated v3 GAPIC client of ResourceManager is now available */
@Deprecated
public class HttpResourceManagerRpc implements ResourceManagerRpc {

  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

  // See doc of create() for more details:
  // https://developers.google.com/resources/api-libraries/documentation/cloudresourcemanager/v1/java/latest/com/google/api/services/cloudresourcemanager/CloudResourceManager.Projects.html#create(com.google.api.services.cloudresourcemanager.model.Project)
  private static final RetrySettings CREATE_RETRY_SETTINGS =
      RetrySettings.newBuilder()
          // SLO permits 30s at 90th percentile, 4x it for total limit.
          // Observed latency is much lower: 11s at 95th percentile.
          .setTotalTimeout(Duration.ofMinutes(2))
          // Linked doc recommends polling at 5th second.
          .setInitialRetryDelay(Duration.ofSeconds(5))
          .setRetryDelayMultiplier(1.5)
          // Observed P95 latency is 11s. We probably shouldn't sleep longer than this.
          .setMaxRetryDelay(Duration.ofSeconds(11))
          .setJittered(true)
          .setInitialRpcTimeout(Duration.ofSeconds(5))
          .setMaxRpcTimeout(Duration.ofSeconds(5))
          .build();

  // reference: https://github.com/googleapis/googleapis/blob/master/google/rpc/code.proto
  private static final ImmutableMap<Integer, Integer> RPC_TO_HTTP_CODES =
      ImmutableMap.<Integer, Integer>builder()
          .put(0, 200)
          .put(1, 499)
          .put(2, 500)
          .put(3, 400)
          .put(4, 504)
          .put(5, 404)
          .put(6, 409)
          .put(7, 403)
          .put(16, 401)
          .put(8, 429)
          .put(9, 400)
          .put(10, 409)
          .put(11, 400)
          .put(12, 501)
          .put(13, 500)
          .put(14, 503)
          .put(15, 500)
          .build();

  private static final ResultRetryAlgorithm<com.google.cloud.resourcemanager.v3.Operation> OPERATION_HANDLER =
      new ResultRetryAlgorithm<com.google.cloud.resourcemanager.v3.Operation>() {
        @Override
        public TimedAttemptSettings createNextAttempt(
            Throwable prevThrowable,
            com.google.cloud.resourcemanager.v3.Operation prevResponse,
            TimedAttemptSettings prevSettings) {
          return null;
        }

        @Override
        public boolean shouldRetry(
            Throwable prevThrowable, com.google.cloud.resourcemanager.v3.Operation prevOp) {
          if (prevThrowable == null) {
            return !prevOp.getDone();
          }
          return prevThrowable instanceof ResourceManagerException
              && ((ResourceManagerException) prevThrowable).isRetryable();
        }
      };

  private final ProjectsClient projectsClient;
  private final OrganizationsClient organizationsClient;
  private final FoldersClient foldersClient;
  private final ApiClock clock;

  public HttpResourceManagerRpc(ResourceManagerOptions options) throws IOException {
    HttpTransportOptions transportOptions = (HttpTransportOptions) options.getTransportOptions();
    HttpRequestInitializer initializer = transportOptions.getHttpRequestInitializer(options);

    ResourceManagerStubSettings.Builder settingsBuilder =
        ResourceManagerStubSettings.newBuilder()
            .setTransportChannelProvider(
                ResourceManagerOptions.defaultGrpcTransportProviderBuilder()
                    .setHttpTransport(transportOptions.getHttpTransportFactory().create())
                    .setHttpRequestInitializer(initializer)
                    .build());

    projectsClient = ProjectsClient.create(settingsBuilder.build());
    organizationsClient = OrganizationsClient.create(settingsBuilder.build());
    foldersClient = FoldersClient.create(settingsBuilder.build());
    clock = options.getClock();
  }

  private static ResourceManagerException translate(IOException exception) {
    return new ResourceManagerException(exception);
  }

  private static ResourceManagerException translate(Status status) {
    Integer code = RPC_TO_HTTP_CODES.get(status.getCode());
    if (code == null) {
      code = BaseHttpServiceException.UNKNOWN_CODE;
    }
    return new ResourceManagerException(code, status.getMessage());
  }

  @Override
  public com.google.cloud.resourcemanager.v3.Project create(
      com.google.cloud.resourcemanager.v3.Project project) {
    final com.google.cloud.resourcemanager.v3.Operation operation;
    try {
      operation = projectsClient.createProjectAsync(project).get();
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }

    com.google.cloud.resourcemanager.v3.Operation finishedOp =
        runWithRetries(
            new Callable<com.google.cloud.resourcemanager.v3.Operation>() {
              @Override
              public com.google.cloud.resourcemanager.v3.Operation call() {
                try {
                  return projectsClient.getOperation(operation.getName());
                } catch (Exception ex) {
                  throw translate(new IOException(ex));
                }
              }
            },
            CREATE_RETRY_SETTINGS,
            OPERATION_HANDLER,
            clock);

    if (finishedOp.hasError()) {
      throw translate(
          new Status()
              .toBuilder()
              .setCode(finishedOp.getError().getCode())
              .setMessage(finishedOp.getError().getMessage())
              .build());
    }

    try {
      return projectsClient.getProject(finishedOp.getName());
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public void delete(String projectId) {
    try {
      projectsClient.deleteProject(projectId);
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public Tuple<String, Iterable<com.google.cloud.resourcemanager.v3.Project>> list(
      Map<Option, ?> options) {
    try {
      ProjectsClient.ListProjectsPagedResponse response = projectsClient.listProjects();
      return Tuple.<String, Iterable<com.google.cloud.resourcemanager.v3.Project>>of(
          response.getNextPageToken(), response.iterateAll());
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public com.google.cloud.resourcemanager.v3.Project get(
      String projectId, Map<Option, ?> options) {
    try {
      return projectsClient.getProject(projectId);
    } catch (Exception ex) {
      ResourceManagerException translated = translate(new IOException(ex));
      if (translated.getCode() == HTTP_FORBIDDEN || translated.getCode() == HTTP_NOT_FOUND) {
        // Service can return either 403 or 404 to signify that the project doesn't exist.
        return null;
      } else {
        throw translated;
      }
    }
  }

  @Override
  public void undelete(String projectId) {
    try {
      projectsClient.undeleteProject(projectId);
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public com.google.cloud.resourcemanager.v3.Project replace(
      com.google.cloud.resourcemanager.v3.Project project) {
    try {
      return projectsClient.updateProject(project);
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public Policy getPolicy(String projectId) throws ResourceManagerException {
    try {
      return projectsClient.getIamPolicy(projectId, GetPolicyOptions.newBuilder().build());
    } catch (Exception ex) {
      ResourceManagerException translated = translate(new IOException(ex));
      if (translated.getCode() == HTTP_FORBIDDEN) {
        // Service returns permission denied if policy doesn't exist.
        return null;
      } else {
        throw translated;
      }
    }
  }

  @Override
  public Policy replacePolicy(String projectId, Policy newPolicy) throws ResourceManagerException {
    try {
      return projectsClient.setIamPolicy(
          projectId, SetIamPolicyRequest.newBuilder().setPolicy(newPolicy).build());
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public List<Boolean> testPermissions(String projectId, List<String> permissions)
      throws ResourceManagerException {
    try {
      TestIamPermissionsResponse response =
          projectsClient.testIamPermissions(
              projectId, TestIamPermissionsRequest.newBuilder().addAllPermissions(permissions).build());
      List<String> permissionsOwned = response.getPermissionsList();
      ImmutableList.Builder<Boolean> answer = ImmutableList.builder();
      for (String p : permissions) {
        answer.add(permissionsOwned.contains(p));
      }
      return answer.build();
    } catch (Exception ex) {
      throw translate(new IOException(ex));
    }
  }

  @Override
  public Map<String, Boolean> testOrgPermissions(String resource, List<String> permissions)
      throws IOException {
    try {
      TestIamPermissionsResponse response =
          organizationsClient.testIamPermissions(
              resource, TestIamPermissionsRequest.newBuilder().addAllPermissions(permissions).build());
      List<String> permissionsOwned = response.getPermissionsList();
      ImmutableMap.Builder<String, Boolean> answer = ImmutableMap.builder();
      for (String permission : permissions) {
        answer.put(permission, permissionsOwned.contains(permission));
      }
      return answer.build();
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }

  @Override
  public void clearOrgPolicy(String resource, OrgPolicy orgPolicy) throws IOException {
    try {
      foldersClient.deleteOrgPolicy(resource, orgPolicy.getConstraint());
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }

  @Override
  public OrgPolicy getEffectiveOrgPolicy(String resource, String constraint) throws IOException {
    try {
      return foldersClient.getEffectiveOrgPolicy(
          GetEffectiveOrgPolicyRequest.newBuilder().setName(resource).setConstraint(constraint).build());
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }

  @Override
  public OrgPolicy getOrgPolicy(String resource, String constraint) throws IOException {
    try {
      return foldersClient.getOrgPolicy(
          GetOrgPolicyRequest.newBuilder().setName(resource).setConstraint(constraint).build());
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }

  @Override
  public ListResult<com.google.cloud.resourcemanager.model.v3.Constraint>
      listAvailableOrgPolicyConstraints(String resource, Map<Option, ?> options)
          throws IOException {
    return ListResult.of(null, ImmutableList.of());
  }

  @Override
  public ListResult<OrgPolicy> listOrgPolicies(String resource, Map<Option, ?> options)
      throws IOException {
    try {
      FoldersClient.ListOrgPoliciesPagedResponse response =
          foldersClient.listOrgPolicies(
              ListOrgPoliciesRequest.newBuilder().setParent(resource).build());
      return ListResult.of(response.getNextPageToken(), response.iterateAll());
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }

  @Override
  public OrgPolicy replaceOrgPolicy(String resource, OrgPolicy orgPolicy) throws IOException {
    try {
      return foldersClient.updateOrgPolicy(orgPolicy);
    } catch (Exception ex) {
      throw ResourceManagerException.translateAndThrow(new IOException(ex));
    }
  }
}