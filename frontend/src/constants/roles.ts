// Role constants for type safety and consistency
export const USER_ROLES = {
  ADMIN: 'admin',
  SELLER: 'seller',
  USER: 'user',
  CUSTOMER: 'customer',
} as const;

export type UserRole = typeof USER_ROLES[keyof typeof USER_ROLES];

// Permission constants for role-based access control
export const PERMISSIONS = {
  CREATE_REVIEW: 'create_review',
  EDIT_OWN_REVIEW: 'edit_own_review',
  DELETE_OWN_REVIEW: 'delete_own_review',
  VIEW_ALL_REVIEWS: 'view_all_reviews',
  MODERATE_REVIEWS: 'moderate_reviews',
  RESPOND_TO_REVIEW: 'respond_to_review',
  MANAGE_SELLER_REVIEWS: 'manage_seller_reviews',
  VIEW_ADMIN_DASHBOARD: 'view_admin_dashboard',
  VIEW_SELLER_DASHBOARD: 'view_seller_dashboard',
} as const;

export type Permission = typeof PERMISSIONS[keyof typeof PERMISSIONS];

// Role-based permission sets
export const ROLE_PERMISSIONS = {
  [USER_ROLES.ADMIN]: [
    PERMISSIONS.CREATE_REVIEW,
    PERMISSIONS.EDIT_OWN_REVIEW,
    PERMISSIONS.DELETE_OWN_REVIEW,
    PERMISSIONS.VIEW_ALL_REVIEWS,
    PERMISSIONS.MODERATE_REVIEWS,
    PERMISSIONS.RESPOND_TO_REVIEW,
    PERMISSIONS.MANAGE_SELLER_REVIEWS,
    PERMISSIONS.VIEW_ADMIN_DASHBOARD,
    PERMISSIONS.VIEW_SELLER_DASHBOARD,
  ],
  [USER_ROLES.SELLER]: [
    PERMISSIONS.CREATE_REVIEW,
    PERMISSIONS.EDIT_OWN_REVIEW,
    PERMISSIONS.DELETE_OWN_REVIEW,
    PERMISSIONS.RESPOND_TO_REVIEW,
    PERMISSIONS.MANAGE_SELLER_REVIEWS,
    PERMISSIONS.VIEW_SELLER_DASHBOARD,
  ],
  [USER_ROLES.USER]: [
    PERMISSIONS.CREATE_REVIEW,
    PERMISSIONS.EDIT_OWN_REVIEW,
    PERMISSIONS.DELETE_OWN_REVIEW,
  ],
  [USER_ROLES.CUSTOMER]: [
    PERMISSIONS.CREATE_REVIEW,
    PERMISSIONS.EDIT_OWN_REVIEW,
    PERMISSIONS.DELETE_OWN_REVIEW,
  ],
} as const;

// Helper functions for role checking
export const isAdmin = (role?: UserRole): boolean => role === USER_ROLES.ADMIN;
export const isSeller = (role?: UserRole): boolean => role === USER_ROLES.SELLER;
export const isUser = (role?: UserRole): boolean => role === USER_ROLES.USER;
export const isCustomer = (role?: UserRole): boolean => role === USER_ROLES.CUSTOMER;
export const isRegularUser = (role?: UserRole): boolean => isUser(role) || isCustomer(role);

// Permission checking functions
export const hasPermission = (role: UserRole, permission: Permission): boolean => {
  if (!role || !permission) return false;
  const rolePermissions = ROLE_PERMISSIONS[role];
  return rolePermissions?.includes(permission as any) || false;
};

export const hasAnyPermission = (role: UserRole, permissions: Permission[]): boolean => {
  if (!role) return false;
  return permissions.some(permission => hasPermission(role, permission));
};

// Role display names for UI
export const ROLE_DISPLAY_NAMES = {
  [USER_ROLES.ADMIN]: 'Administrator',
  [USER_ROLES.SELLER]: 'Seller',
  [USER_ROLES.USER]: 'User',
  [USER_ROLES.CUSTOMER]: 'Customer',
} as const;

// Role color schemes for UI
export const ROLE_COLORS = {
  [USER_ROLES.ADMIN]: {
    primary: '#ef4444', // red
    secondary: '#fef2f2', // light red
    text: '#991b1b',   // dark red
  },
  [USER_ROLES.SELLER]: {
    primary: '#3b82f6', // blue
    secondary: '#eff6ff', // light blue
    text: '#1e40af',   // dark blue
  },
  [USER_ROLES.USER]: {
    primary: '#10b981', // green
    secondary: '#ecfdf5', // light green
    text: '#047857',   // dark green
  },
  [USER_ROLES.CUSTOMER]: {
    primary: '#10b981', // green (same as user)
    secondary: '#ecfdf5', // light green
    text: '#047857',   // dark green
  },
} as const;