import React from 'react'
import CIcon from '@coreui/icons-react'

const _nav =  [
  {
    _tag: 'CSidebarNavItem',
    name: 'Dashboard',
    to: '/dashboard',
    icon: <CIcon name="cil-speedometer" customClasses="c-sidebar-nav-icon"/>,
  },
  {
    _tag: 'CSidebarNavTitle',
    _children: ['Collections']
  },
  {
    _tag: 'CSidebarNavItem',
    name: 'Collections',
    to: '/collections',
    icon: 'cil-drop',
  },
  {
    _tag: 'CSidebarNavTitle',
    _children: ['Settings']
  },
  {
    _tag: 'CSidebarNavItem',
    name: 'Webhooks',
    to: '/Webhooks',
    icon: 'cil-puzzle',
  },
  {
    _tag: 'CSidebarNavItem',
    name: 'Security',
    to: '/security',
    icon: 'cil-cursor',
  },
  {
    _tag: 'CSidebarNavItem',
    name: 'Swagger',
    to: '/q/swagger-ui',
    icon: 'cil-chart-pie'
  },
  {
    _tag: 'CSidebarNavDivider',
    className: 'm-2'
  }
]

export default _nav
