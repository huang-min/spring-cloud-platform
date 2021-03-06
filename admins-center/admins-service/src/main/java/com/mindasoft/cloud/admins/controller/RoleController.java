package com.mindasoft.cloud.admins.controller;

import com.mindasoft.cloud.admins.entity.RoleEntity;
import com.mindasoft.cloud.admins.service.RoleMenuService;
import com.mindasoft.cloud.admins.service.RoleService;
import com.mindasoft.cloud.commons.util.PageUtils;
import com.mindasoft.cloud.commons.util.R;
import com.mindasoft.cloud.commons.validator.ValidatorUtils;
import com.mindasoft.cloud.security.util.OAuth2Utils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * 角色
 *
 * @author huangmin
 * @email hmiter@sina.com
 * @date 2018-12-24 13:02:21
 */
@RestController
@RequestMapping("role")
@Api(tags = "角色api")
public class RoleController {
    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleMenuService roleMenuService;

    /**
     * 角色列表
     */
    @GetMapping("/select")
    @PreAuthorize("hasAuthority('admins:role:select')")
    public R select(){
        Map<String, Object> map = new HashMap<>();

        //如果不是超级管理员，则只查询自己所拥有的角色列表
        Long adminId = OAuth2Utils.getAdminId();
        if(1 != adminId){
            map.put("createAdminId", adminId);
        }
        List<RoleEntity> list = roleService.selectByMap(map);
        return R.ok().put(list);
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('admins:role:list')")
    @ApiOperation(value = "列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页起始位置", required = true),
            @ApiImplicitParam(name = "limit",value = "分页结束位置", required = true)
    })
    public R list(@RequestParam Map<String, Object> params){
        //如果不是超级管理员，则只查询自己创建的角色列表
        Long adminId = OAuth2Utils.getAdminId();
        if(1 != adminId){
            params.put("createAdminId", adminId);
        }

        PageUtils page = roleService.queryPage(params);
        return R.ok().put(page);
    }


    /**
     * 信息
     */
    @GetMapping("/info/{roleId}")
    @PreAuthorize("hasAuthority('admins:role:info')")
    @ApiOperation(value = "信息")
    public R info(@PathVariable("roleId") Long roleId){
        RoleEntity role = roleService.selectById(roleId);

        //查询角色对应的菜单
        List<Long> menuIdList = roleMenuService.queryMenuIdList(roleId);
        role.setMenuIdList(menuIdList);

        return R.ok().put(role);
    }

    /**
     * 保存
     */
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('admins:role:save')")
    @ApiOperation(value = "保存")
    public R save(@RequestBody RoleEntity role){
        ValidatorUtils.validateEntity(role);
        role.setCreateAdminId(OAuth2Utils.getAdminId());

        roleService.save(role);
        return R.ok();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    @PreAuthorize("hasAuthority('admins:role:update')")
    @ApiOperation(value = "修改")
    public R update(@RequestBody RoleEntity role){
        ValidatorUtils.validateEntity(role);
        role.setCreateAdminId(OAuth2Utils.getAdminId());

        roleService.update(role);
        return R.ok();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('admins:role:delete')")
    @ApiOperation(value = "删除")
    public R delete(@RequestBody Long[] roleIds){
        roleService.deleteBatch(roleIds);
        return R.ok();
    }

}
