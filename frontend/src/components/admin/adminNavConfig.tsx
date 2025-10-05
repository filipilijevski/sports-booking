import * as React from 'react';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';
import StorefrontIcon from '@mui/icons-material/Storefront';
import Inventory2Icon from '@mui/icons-material/Inventory2';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import EventIcon from '@mui/icons-material/Event';
import HowToRegIcon from '@mui/icons-material/HowToReg';
import ChecklistRtlIcon from '@mui/icons-material/ChecklistRtl';
import ArticleIcon from '@mui/icons-material/Article';
import TableRestaurantIcon from '@mui/icons-material/TableRestaurant';
import AssessmentIcon from '@mui/icons-material/Assessment';
import PersonIcon from '@mui/icons-material/Person';
import LogoutIcon from '@mui/icons-material/Logout';

export type AdminNavItem = {
  label: string;
  to?: string;               // internal route
  icon?: React.ReactNode;
  action?: 'logout';         // special action item
};

export type AdminNavSection = {
  label: string;
  to?: string;               // optional direct link when section is a main page
  items?: AdminNavItem[];    // children (expand/collapse)
  disabled?: boolean;
};

export const ADMIN_NAV_SECTIONS: AdminNavSection[] = [
  /* (Main element) - Overview */
  {
    label: 'Overview',
    to: '/admin',
    items: [],
  },

  /* (Main element) - Customers */
  {
    label: 'Customers',
    items: [
      { label: 'Profiles', to: '/admin/profiles', icon: <PeopleAltIcon /> }, // AdminUsers.tsx
    ],
  },

  /* (Main element) - Shop */
  {
    label: 'Shop',
    items: [
      { label: 'Products',           to: '/admin/shop',         icon: <Inventory2Icon /> }, // AdminShop.tsx
      { label: 'In-Person Checkout', to: '/admin/manual-shop',  icon: <PointOfSaleIcon /> }, // AdminManualShop.tsx
      { label: 'Orders & Refunds',   to: '/admin/orders',       icon: <ReceiptLongIcon /> }, // AdminOrders.tsx
      { label: 'Coupons',            to: '/admin/coupons',      icon: <LocalOfferIcon /> },  // AdminCouponsPanel.tsx
      { label: 'Product Audit Log',  to: '/admin/shop-audit',   icon: <ChecklistRtlIcon /> }, // AdminShopAudit.tsx
    ],
  },

  /* (Main element) - Booking */
  {
    label: 'Booking',
    items: [
      { label: 'Create Program Plans',   to: '/admin/booking/program-plans',     icon: <EventIcon /> },
      { label: 'Create Membership Plans',to: '/admin/booking/membership-plans',  icon: <AssessmentIcon /> },
      { label: 'Table Rental Credits',   to: '/admin/booking/table-credits',     icon: <TableRestaurantIcon /> },
      { label: 'Manual Enrollment',      to: '/admin/enroll',                    icon: <HowToRegIcon /> },
      { label: 'Attendance',             to: '/admin/schedules',                 icon: <ChecklistRtlIcon /> },
    ],
  },

  /* (Main element) - Blogs and Posts */
  {
    label: 'Blogs and Posts',
    items: [
      { label: 'Manage News Posts', to: '/admin/blog', icon: <ArticleIcon /> }, // AdminBlog.tsx
    ],
  },

  /* (Main element) - Reports */
  {
    label: 'Reports',
    to: '/admin/reports',
    items: [],
  },
];

export const ADMIN_SECONDARY_ITEMS: AdminNavItem[] = [
  { label: 'My Profile', to: '/admin/profile', icon: <PersonIcon /> },
  { label: 'Logout',     action: 'logout', icon: <LogoutIcon /> },
];

// Optional top-level pictogram for context (not part of links)
export const OVERVIEW_ICON = <DashboardIcon />;
export const SHOP_ICON = <StorefrontIcon />;
