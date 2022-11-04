package org.dhorse.infrastructure.strategy.repo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dhorse.api.enums.MessageCodeEnum;
import org.dhorse.api.result.PageData;
import org.dhorse.api.vo.GlobalConfigAgg.CodeRepo;
import org.dhorse.api.vo.ProjectBranch;
import org.dhorse.infrastructure.strategy.repo.param.BranchListParam;
import org.dhorse.infrastructure.strategy.repo.param.BranchPageParam;
import org.dhorse.infrastructure.utils.DeployContext;
import org.dhorse.infrastructure.utils.LogUtils;
import org.gitlab4j.api.Constants.MergeRequestState;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.MergeRequestApi;
import org.gitlab4j.api.models.Branch;
import org.gitlab4j.api.models.MergeRequest;
import org.gitlab4j.api.models.MergeRequestFilter;
import org.gitlab4j.api.models.MergeRequestParams;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.TreeItem;
import org.gitlab4j.api.models.TreeItem.Type;


/**
 * 
 * GitLab代码仓库
 * 
 * @author Dahai 2021-9-20 19:14:08
 */
public class GitLabCodeRepoStrategy extends CodeRepoStrategy {

	@Override
	public boolean doDownloadBranch(DeployContext context) {
		int projectId = Integer.parseInt(context.getProject().getCodeRepoPath());
		String branchName = context.getBranchName();
		GitLabApi gitLabApi = gitLabApi(context.getGlobalConfigAgg().getCodeRepo());
		try {
			Project project = gitLabApi.getProjectApi().getProject(projectId);
			if (project == null) {
				logger.warn("The project does not exist, project id : {}", projectId);
				gitLabApi.close();
				return false;
			}

			String localPathOfBranch = checkLocalPathOfBranch(context);
			if (localPathOfBranch == null) {
				logger.warn("Failed to local path of branch, project id : {}", projectId);
				gitLabApi.close();
				return false;
			}

			List<TreeItem> trees = gitLabApi.getRepositoryApi().getTree(projectId, null, branchName, true);
			for (TreeItem tree : trees) {
				File file = new File(localPathOfBranch + tree.getPath());
				if (Type.BLOB.equals(tree.getType())) {
					if (!file.exists()) {
						file.createNewFile();
					}
					byte[] buffer = new byte[1024 * 1024];
					int length = 0;
					try(InputStream inputStream = gitLabApi.getRepositoryFileApi().getRawFile(projectId, branchName,
							tree.getPath());
							FileOutputStream outStream = new FileOutputStream(file)){
						while ((length = inputStream.read(buffer)) != -1) {
							outStream.write(buffer, 0, length);
						}
					}
				} else if (Type.TREE.equals(tree.getType()) && !file.exists()) {
					file.mkdir();
				}
			}
			context.setLocalPathOfBranch(localPathOfBranch);
		} catch (Exception e) {
			logger.error("Failed to download branch", e);
			return false;
		} finally {
			gitLabApi.close();
		}
		return true;
	}

	public void mergeBranch(DeployContext context) {
		GitLabApi gitLabApi = gitLabApi(context.getGlobalConfigAgg().getCodeRepo());
		try {
			Branch branch = gitLabApi.getRepositoryApi().getBranch(context.getProject().getCodeRepoPath(), context.getBranchName());
			if(branch.getMerged() != null && branch.getMerged().booleanValue()) {
				logger.error("The branch has been merged");
				return;
			}
		} catch (GitLabApiException e) {
			LogUtils.throwException(logger, e, MessageCodeEnum.PROJECT_BRANCH_FAILURE);
		}
		
		MergeRequestApi mergeRequestApi = gitLabApi.getMergeRequestApi();
		MergeRequestFilter filter = new MergeRequestFilter();
		filter.setState(MergeRequestState.OPENED);
		filter.setProjectId(Integer.valueOf(context.getProject().getCodeRepoPath()));
		MergeRequest mergeRequest = null;
		try {
			//1.如果存在未合并的请求，直接报错
			Stream<MergeRequest> stream = mergeRequestApi.getMergeRequestsStream(filter);
			if (stream != null && stream.count() > 0) {
				LogUtils.throwException(logger, MessageCodeEnum.UNFINISHED_MERGE_REQUEST_EXSITS);
			}
			// 提交合请求
			MergeRequestParams mergeRequestParams = new MergeRequestParams();
			mergeRequestParams.withSourceBranch(context.getBranchName());
			mergeRequestParams.withTargetBranch("master");
			mergeRequestParams.withTitle("dhorse merge");
			mergeRequest = mergeRequestApi.createMergeRequest(context.getProject().getCodeRepoPath(),
					mergeRequestParams);
			// 接受合并请求
			mergeRequestApi.acceptMergeRequest(context.getProject().getCodeRepoPath(), mergeRequest.getIid());
		} catch (GitLabApiException e) {
			//如果合并失败，则关闭本次合并请求，目前由人工介入处理。
//			if(mergeRequest != null) {
//				try {
//					mergeRequestApi.cancelMergeRequest(context.getProject().getCodeRepoPath(), mergeRequest.getIid());
//				} catch (GitLabApiException e1) {
//					LogUtils.throwException(logger, MessageCodeEnum.MERGE_BRANCH);
//				}
//			}
			LogUtils.throwException(logger, e, MessageCodeEnum.MERGE_BRANCH);
		} finally {
			gitLabApi.close();
		}
	}

	@Override
	public void createBranch(CodeRepo codeRepo, String codeRepoPath, String branchName) {
		GitLabApi gitLabApi = gitLabApi(codeRepo);
		try {
			gitLabApi.getRepositoryApi().createBranch(codeRepoPath, branchName, "master");
		} catch (GitLabApiException e) {
			LogUtils.throwException(logger, e, MessageCodeEnum.CREATE_BRANCH_FAILURE);
		} finally {
			gitLabApi.close();
		}
	}

	@Override
	public void deleteBranch(CodeRepo codeRepo, String codeRepoPath, String branchName) {
		GitLabApi gitLabApi = gitLabApi(codeRepo);
		try {
			gitLabApi.getRepositoryApi().deleteBranch(codeRepoPath, branchName);
		} catch (GitLabApiException e) {
			LogUtils.throwException(logger, e, MessageCodeEnum.DELETE_BRANCH_FAILURE);
		} finally {
			gitLabApi.close();
		}
	}
	
	@Override
	public PageData<ProjectBranch> branchPage(CodeRepo codeRepo, BranchPageParam param) {
		GitLabApi gitLabApi = gitLabApi(codeRepo);
		List<Branch> list = null;
		try {
			list = gitLabApi.getRepositoryApi().getBranches(param.getProjectIdOrPath(), param.getBranchName());
		} catch (GitLabApiException e) {
			LogUtils.throwException(logger, e, MessageCodeEnum.PROJECT_BRANCH_PAGE_FAILURE);
		} finally {
			gitLabApi.close();
		}
		
		PageData<ProjectBranch> pageData = new PageData<>();
		int dataCount = list.size();
		if (dataCount == 0) {
			pageData.setPageNum(1);
			pageData.setPageCount(0);
			pageData.setPageSize(param.getPageSize());
			pageData.setItemCount(0);
			return pageData;
		}
		
		//按照提交时间倒排序
		list.sort((e1, e2) -> e2.getCommit().getCommittedDate().compareTo(e1.getCommit().getCommittedDate()));
		
		int pageCount = dataCount / param.getPageSize();
		if (dataCount % param.getPageSize() > 0) {
			pageCount += 1;
		}
		int pageNum = param.getPageNum() > pageCount ? pageCount : param.getPageNum();
		int startOffset = (pageNum - 1) * param.getPageSize();
		int endOffset = pageNum * param.getPageSize();
		endOffset = endOffset > dataCount ? dataCount : endOffset;
		List<Branch> page = list.subList(startOffset, endOffset);
		pageData.setItems(page.stream().map(e ->{
			ProjectBranch projectBranch = new ProjectBranch();
			projectBranch.setBranchName(e.getName());
			projectBranch.setMergedStatus(e.getMerged() ? 1 : 0);
			projectBranch.setUpdateTime(e.getCommit().getCommittedDate());
			return projectBranch;
		}).collect(Collectors.toList()));
		pageData.setPageNum(pageNum);
		pageData.setPageCount(pageCount);
		pageData.setPageSize(param.getPageSize());
		pageData.setItemCount(dataCount);
		
		return pageData;
	}
	
	@Override
	public List<ProjectBranch> branchList(CodeRepo codeRepo, BranchListParam param) {
		GitLabApi gitLabApi = gitLabApi(codeRepo);
		List<Branch> list = null;
		try {
			list = gitLabApi.getRepositoryApi().getBranches(param.getProjectIdOrPath(), param.getBranchName());
		} catch (GitLabApiException e) {
			LogUtils.throwException(logger, e, MessageCodeEnum.PROJECT_BRANCH_PAGE_FAILURE);
		} finally {
			gitLabApi.close();
		}
		
		int dataCount = list.size();
		if (dataCount == 0) {
			return Collections.emptyList();
		}
		//按照提交时间倒排序
		list.sort((e1, e2) -> e2.getCommit().getCommittedDate().compareTo(e1.getCommit().getCommittedDate()));
		return list.stream().map(e ->{
			ProjectBranch projectBranch = new ProjectBranch();
			projectBranch.setBranchName(e.getName());
			projectBranch.setMergedStatus(e.getMerged() ? 1 : 0);
			projectBranch.setUpdateTime(e.getCommit().getCommittedDate());
			return projectBranch;
		}).collect(Collectors.toList());
	}
	
	private GitLabApi gitLabApi(CodeRepo codeRepo) {
		GitLabApi gitLabApi = new GitLabApi(codeRepo.getUrl(), codeRepo.getAuthToken());
		gitLabApi.setRequestTimeout(1000, 5 * 1000);
		try {
			gitLabApi.getVersion();
		}catch(GitLabApiException e) {
			//如果token无效，则用账号登录
			if(e.getHttpStatus() == 401 && !StringUtils.isBlank(codeRepo.getAuthUser())) {
				gitLabApi = new GitLabApi(codeRepo.getUrl(), codeRepo.getAuthUser(), codeRepo.getAuthPassword());
				gitLabApi.setRequestTimeout(1000, 5 * 1000);
			}
		}
		
		return gitLabApi;
	}
}