package org.dhorse.rest.resource;

import java.util.List;

import org.dhorse.api.param.project.env.ProjectEnvCreationParam;
import org.dhorse.api.param.project.env.ProjectEnvDeletionParam;
import org.dhorse.api.param.project.env.ProjectEnvPageParam;
import org.dhorse.api.param.project.env.ProjectEnvSearchParam;
import org.dhorse.api.param.project.env.ProjectEnvQueryParam;
import org.dhorse.api.param.project.env.ProjectEnvResoureUpdateParam;
import org.dhorse.api.param.project.env.ProjectEnvUpdateParam;
import org.dhorse.api.param.project.env.TraceUpdateParam;
import org.dhorse.api.result.PageData;
import org.dhorse.api.result.RestResponse;
import org.dhorse.api.vo.ProjectEnv;
import org.dhorse.application.service.ProjectEnvApplicationService;
import org.dhorse.infrastructure.exception.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 
 * 项目环境
 * 
 * @author Dahai
 */
@RestController
@RequestMapping("/project/env")
public class ProjectEnvRest extends AbstractRest {

	@Autowired
	private ProjectEnvApplicationService projectEnvApplicationService;

	/**
	 * 分页查询
	 * 
	 * @param projectEnvPageParam 分页参数
	 * @return 符合条件的分页数据
	 */
	@PostMapping("/page")
	public RestResponse<PageData<ProjectEnv>> page(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvPageParam projectEnvPageParam) {
		try {
			return success(
					projectEnvApplicationService.page(queryLoginUserByToken(loginToken), projectEnvPageParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

	/**
	 * 搜索列表
	 * 
	 * @param projectEnvSearchParam 搜索参数
	 * @return 符合条件的数据
	 */
	@PostMapping("/search")
	public RestResponse<List<ProjectEnv>> search(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvSearchParam projectEnvSearchParam) {
		try {
			return success(
					projectEnvApplicationService.search(queryLoginUserByToken(loginToken), projectEnvSearchParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}
	
	/**
	 * 查询
	 * 
	 * @param projectEnvPageParam 查询
	 * @return 符合条件的数据
	 */
	@PostMapping("/query")
	public RestResponse<ProjectEnv> query(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvQueryParam projectEnvQueryParam) {
		try {
			return success(
					projectEnvApplicationService.query(queryLoginUserByToken(loginToken), projectEnvQueryParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

	/**
	 * 添加
	 * 
	 * @param projectEnvCreattionParam 添加项目参数
	 * @return 无
	 */
	@PostMapping("/add")
	public RestResponse<Void> add(@RequestBody ProjectEnvCreationParam projectEnvCreattionParam) {
		try {
			return success(projectEnvApplicationService.add(projectEnvCreattionParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

	/**
	 * 修改
	 * 
	 * @param projectEnvUpdateParam 修改参数
	 * @return 无
	 */
	@PostMapping("/update")
	public RestResponse<Void> update(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvUpdateParam projectEnvUpdateParam) {
		try {
			return success(
					projectEnvApplicationService.update(queryLoginUserByToken(loginToken), projectEnvUpdateParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

	/**
	 * 修改资源
	 * 
	 * @param envResoureUpdateParam 修改参数
	 * @return 无
	 */
	@PostMapping("/updateResource")
	public RestResponse<Void> updateResource(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvResoureUpdateParam envResoureUpdateParam) {
		try {
			return success(projectEnvApplicationService.updateResource(queryLoginUserByToken(loginToken),
					envResoureUpdateParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}
	
	/**
	 * 设置链路跟踪状态
	 * 
	 * @param openTraceParam 修改参数
	 * @return 无
	 */
	@PostMapping("/updateTrace")
	public RestResponse<Void> updateTrace(@CookieValue("login_token") String loginToken,
			@RequestBody TraceUpdateParam updateTraceParam) {
		try {
			return success(projectEnvApplicationService.updateTrace(queryLoginUserByToken(loginToken),
					updateTraceParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

	/**
	 * 删除
	 * 
	 * @param projectEnvDeletionParam 删除参数
	 * @return 无
	 */
	@PostMapping("/delete")
	public RestResponse<Void> delete(@CookieValue("login_token") String loginToken,
			@RequestBody ProjectEnvDeletionParam projectEnvDeletionParam) {
		try {
			return success(
					projectEnvApplicationService.delete(queryLoginUserByToken(loginToken), projectEnvDeletionParam));
		} catch (ApplicationException e) {
			return this.error(e);
		}
	}

}