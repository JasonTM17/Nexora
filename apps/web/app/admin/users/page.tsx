import { AdminDirectory } from "../admin-directory";

export const dynamic = "force-dynamic";

export default function AdminUsersPage() {
  return <AdminDirectory mode="users" />;
}
