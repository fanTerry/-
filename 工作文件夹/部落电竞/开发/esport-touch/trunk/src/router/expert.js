//专家模块
const expert = () => import('../views/expert/index.vue');
const expertHome = () => import('../views/expert/home/expertHome.vue');
const expertApply = () => import('../views/expert/apply/expertApply.vue');
const expertAgreement = () => import('../views/expert/agreement/expertAgreement.vue');
const articleDetail = () => import('../views/expert/articleDetail/expertArticledetail.vue');

export default [{
    path: '/expert',
    name: 'expert',
    component: expert,
    meta: {
        keepAlive: true
    }
}, {
    path: '/expertHome',
    name: 'expertHome',
    component: expertHome,
    meta: {
        requiresAuth: true
    }
}, {
    path: '/expertApply',
    name: 'expertApply',
    component: expertApply,
    meta: {
        requiresAuth: true,
        keepAlive: true,
    }
}, {
    path: '/expertAgreement',
    name: 'expertAgreement',
    component: expertAgreement
}, {
    path: '/articleDetail',
    name: 'articleDetail',
    component: articleDetail,
    meta: {
        requiresAuth: true
    }
}]
