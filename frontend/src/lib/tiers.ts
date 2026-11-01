import type { MembershipTier } from '../api/types'

export const TIERS: MembershipTier[] = ['STUDENT', 'FACULTY', 'GUEST']

export const tierName = (t: MembershipTier) => t.charAt(0) + t.slice(1).toLowerCase()
