import { login, logout, getInfo, loadMenu } from '@/api/login'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isURL } from '@/utils/validate'

// 开发环境不使用懒加载, 因为懒加载页面太多的话会造成webpack热更新太慢, 所以只有生产环境使用懒加载
const _import = require('@/router/import-' + process.env.NODE_ENV)

const user = {
  state: {
    token: getToken(),
    name: '',
    avatar: '',
    roles: [],
    permissions: [],
    dynamicRouters: []
  },

  mutations: {
    SET_TOKEN: (state, token) => {
      state.token = token
    },
    SET_NAME: (state, name) => {
      state.name = name
    },
    SET_AVATAR: (state, avatar) => {
      state.avatar = avatar
    },
    SET_ROLES: (state, roles) => {
      state.roles = roles
    },
    SET_PERMISSIONS: (state, permissions) => {
      state.permissions = permissions
    },
    SET_DYNAMICROUTERS: (state, dynamicRouters) => {
      state.dynamicRouters = dynamicRouters
    }
  },

  actions: {
    // 登录
    Login({ commit }, userInfo) {
      const username = userInfo.username.trim()
      return new Promise((resolve, reject) => {
        login(username, userInfo.password).then(response => {
          if (response.access_token) {
            setToken(response.access_token)
            commit('SET_TOKEN', response.access_token)
            resolve()
          } else {
            reject('登录失败')
          }
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 获取用户信息
    GetInfo({ commit, state }) {
      return new Promise((resolve, reject) => {
        getInfo(state.token).then(response => {
          const data = response.data
          commit('SET_NAME', data.nickname)
          commit('SET_AVATAR', data.avatar)
          if (data.roles && data.roles.length > 0) { // 验证返回的roles是否是一个非空数组
            commit('SET_ROLES', data.roles)
          } else {
            reject('getInfo: roles must be a non-null array !')
          }
          commit('SET_PERMISSIONS', data.permissions)
          resolve(response)
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 加载菜单
    LoadMenu({ commit, state }) {
      return new Promise((resolve, reject) => {
        loadMenu().then(response => {
          if (response && response.code === 0) {
            const dynamicMenuRoutes = filterDynamicMenuRoutes(response.data)
            commit('SET_DYNAMICROUTERS', dynamicMenuRoutes)
            resolve(response)
          } else {
            reject('LoadMenu: LoadMenu error !')
          }
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 登出
    LogOut({ commit, state }) {
      return new Promise((resolve, reject) => {
        logout(state.token).then(() => {
          commit('SET_TOKEN', '')
          commit('SET_ROLES', [])
          commit('SET_PERMISSIONS', [])
          commit('SET_DYNAMICROUTERS', [])
          removeToken()
          resolve()
        }).catch(error => {
          reject(error)
        })
      })
    },

    // 前端 登出
    FedLogOut({ commit }) {
      return new Promise(resolve => {
        commit('SET_TOKEN', '')
        removeToken()
        resolve()
      })
    }
  }
}

function filterDynamicMenuRoutes(menuList = []) {
  var temp = []
  var routes = []
  for (var i = 0; i < menuList.length; i++) {
    var route = {
      path: '/' + (menuList[i].url ? menuList[i].url : menuList[i].menuId),
      component: _import('layout/Layout'),
      name: menuList[i].name,
      meta: {
        menuId: menuList[i].menuId,
        title: menuList[i].name,
        icon: menuList[i].icon,
        isDynamic: true,
        isTab: true
      },
      children: []
    }
    // 如果有子菜单，必然是父节点
    if (menuList[i].list && menuList[i].list.length >= 1) {
      temp = menuList[i].list
      route['children'] = filterDynamicMenuRoutes(temp)
      route['redirect'] = '/' + temp[0].url
    } else if (menuList[i].url && !isURL(menuList[i].url)) {
      // url不为空并且url不以http[s]://开头
      route['component'] = _import(`${menuList[i].url}`)
    } else if (!menuList[i].url) {
      // url为空的
      route['path'] = `/i-${menuList[i].menuId}`
      route['component'] = _import('404')
    } else if (isURL(menuList[i].url)) {
      // url以http[s]://开头
      route['path'] = `/i-${menuList[i].menuId}`
      route['component'] = _import('frame')
      route['props'] = { iframeUrl: (isURL(menuList[i].url) ? menuList[i].url : '') }
    }
    // 其他的是以http开头的
    routes.push(route)
  }
  // console.info(JSON.stringify(routes))
  return routes
}

export default user
