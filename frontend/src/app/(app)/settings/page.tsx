"use client";

import { type FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import { Eye, Loader2, PencilLine, PlusCircle, Save, Shield, UserCog, Users, X } from "lucide-react";
import { toast } from "sonner";

import { PageHeader } from "@/components/erm/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { FixedInputField, LabeledSelectField } from "@/components/ui/form-fields";
import { Spinner } from "@/components/ui/spinner";
import { loadSession } from "@/lib/auth-storage";
import {
  ApiError,
  createOrganizationRoleConfig,
  createReportingManagerConfig,
  getOrganizationConfigReportingManagerConfigs,
  getOrganizationRoleConfig,
  type OrganizationConfigResponse,
  type RoleConfig,
  type RoleConfigRequest,
  type ReportingManagerConfig,
  type ReportingManagerConfigRequest,
  type RoleSummary,
  updateOrganizationRoleConfig,
  updateReportingManagerConfig,
} from "@/lib/api";

type ReportingManagerForm = {
  designationRoleName: string;
  reportsToRoleName: string;
};

type ModalMode = "create" | "edit";
type RoleModalMode = "create" | "edit";

const initialForm: ReportingManagerForm = {
  designationRoleName: "",
  reportsToRoleName: "",
};

const initialRoleForm = {
  roleName: "",
  roleDescription: "",
};

function rowStatus(row: ReportingManagerConfig) {
  return row.reportsToRoleName ? "Configured" : "Not configured";
}

function canMapRole(roleName: string, configs: ReportingManagerConfig[]) {
  return !configs.some((item) => item.designationRoleName.toLowerCase() === roleName.toLowerCase());
}

export default function SettingsPage() {
  const accessToken = useMemo(() => loadSession()?.accessToken ?? null, []);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [data, setData] = useState<OrganizationConfigResponse | null>(null);
  const [activeSubTab, setActiveSubTab] = useState<"reporting-manager-config" | "role-config">("reporting-manager-config");
  const [modalMode, setModalMode] = useState<ModalMode | null>(null);
  const [editingConfig, setEditingConfig] = useState<ReportingManagerConfig | null>(null);
  const [form, setForm] = useState<ReportingManagerForm>(initialForm);
  const [roleModalMode, setRoleModalMode] = useState<RoleModalMode | null>(null);
  const [editingRole, setEditingRole] = useState<RoleConfig | null>(null);
  const [viewRole, setViewRole] = useState<RoleConfig | null>(null);
  const [roleForm, setRoleForm] = useState(initialRoleForm);
  const [reportingSearch, setReportingSearch] = useState("");
  const [roleSearch, setRoleSearch] = useState("");

  const loadData = useCallback(async () => {
    if (!accessToken) {
      toast.error("Session not found. Please login again.");
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const response = await getOrganizationConfigReportingManagerConfigs(accessToken);
      setData(response);
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "Unable to load organization configuration.";
      toast.error(message);
    } finally {
      setLoading(false);
    }
  }, [accessToken]);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  const roleOptions = useMemo(() => data?.roles ?? [], [data]);
  const configs = useMemo(() => data?.reportingManagerConfigs ?? [], [data]);
  const roleConfigs = useMemo(() => data?.roleConfigs ?? [], [data]);

  const filteredReportingConfigs = useMemo(() => {
    const query = reportingSearch.trim().toLowerCase();
    if (!query) {
      return configs;
    }
    return configs.filter((row) => {
      return (
          row.designationRoleName.toLowerCase().includes(query) ||
          (row.reportsToRoleName ?? "").toLowerCase().includes(query)
      );
    });
  }, [configs, reportingSearch]);

  const filteredRoleConfigs = useMemo(() => {
    const query = roleSearch.trim().toLowerCase();
    if (!query) {
      return roleConfigs;
    }
    return roleConfigs.filter((role) => {
      return (
          role.roleName.toLowerCase().includes(query) ||
          (role.roleDescription ?? "").toLowerCase().includes(query)
      );
    });
  }, [roleConfigs, roleSearch]);

  const configSummary = useMemo(() => {
    const editableRoles = roleConfigs.filter((role) => role.roleNameEditable).length;
    const lockedRoles = roleConfigs.length - editableRoles;
    const reportingMappings = configs.filter((row) => row.reportsToRoleName).length;
    return {
      totalRoles: roleConfigs.length,
      editableRoles,
      lockedRoles,
      reportingMappings,
    };
  }, [configs, roleConfigs]);

  const availableCreateRoles = useMemo(
      () => roleOptions.filter((role) => canMapRole(role.name, configs)),
      [roleOptions, configs]
  );

  const reportToOptions = useMemo(() => {
    if (!form.designationRoleName) {
      return roleOptions;
    }
    return roleOptions.filter((role) => role.name.toLowerCase() !== form.designationRoleName.toLowerCase());
  }, [form.designationRoleName, roleOptions]);

  function openCreateModal(defaultRoleName = "") {
    setModalMode("create");
    setEditingConfig(null);
    setForm({
      designationRoleName: defaultRoleName,
      reportsToRoleName: "",
    });
  }

  function openEditModal(row: ReportingManagerConfig) {
    setModalMode("edit");
    setEditingConfig(row);
    setForm({
      designationRoleName: row.designationRoleName,
      reportsToRoleName: row.reportsToRoleName ?? "",
    });
  }

  function closeModal() {
    setModalMode(null);
    setEditingConfig(null);
    setForm(initialForm);
  }

  function openCreateRoleModal() {
    setRoleModalMode("create");
    setEditingRole(null);
    setRoleForm(initialRoleForm);
  }

  function openEditRoleModal(role: RoleConfig) {
    setRoleModalMode("edit");
    setEditingRole(role);
    setRoleForm({
      roleName: role.roleName,
      roleDescription: role.roleDescription ?? "",
    });
  }

  async function openViewRole(roleId: number) {
    if (!accessToken) {
      return;
    }
    try {
      const role = await getOrganizationRoleConfig(accessToken, roleId);
      setViewRole(role);
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "Unable to load role details.";
      toast.error(message);
    }
  }

  function closeRoleModal() {
    setRoleModalMode(null);
    setEditingRole(null);
    setRoleForm(initialRoleForm);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || !modalMode) {
      return;
    }

    if (!form.designationRoleName.trim() || !form.reportsToRoleName.trim()) {
      toast.error("Please select both roles.");
      return;
    }

    setSaving(true);
    try {
      const payload: ReportingManagerConfigRequest = {
        designationRoleName: form.designationRoleName.trim(),
        reportsToRoleName: form.reportsToRoleName.trim(),
      };

      if (modalMode === "create") {
        await createReportingManagerConfig(accessToken, payload);
        toast.success("Reporting manager config created.");
      } else if (editingConfig?.id) {
        await updateReportingManagerConfig(accessToken, editingConfig.id, payload);
        toast.success("Reporting manager config updated.");
      }

      closeModal();
      await loadData();
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "Unable to save reporting manager config.";
      toast.error(message);
    } finally {
      setSaving(false);
    }
  }

  async function handleRoleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!accessToken || !roleModalMode) {
      return;
    }

    if (!roleForm.roleName.trim()) {
      toast.error("Role name is required.");
      return;
    }

    setSaving(true);
    try {
      const payload: RoleConfigRequest = {
        roleName: roleForm.roleName.trim(),
        roleDescription: roleForm.roleDescription.trim(),
      };

      if (roleModalMode === "create") {
        await createOrganizationRoleConfig(accessToken, payload);
        toast.success("Role created.");
      } else if (editingRole) {
        await updateOrganizationRoleConfig(accessToken, editingRole.id, payload);
        toast.success("Role updated.");
      }

      closeRoleModal();
      await loadData();
    } catch (error) {
      const message = error instanceof ApiError ? error.message : "Unable to save role.";
      toast.error(message);
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
        <div className="flex min-h-[40vh] items-center justify-center">
          <Spinner label="Loading organization configuration..." />
        </div>
    );
  }

  return (
      <>
        <PageHeader
            description="Manage portal configuration and reporting hierarchy access for admin roles."
            title="Organization Configuration"
        />

        <div className="mb-5 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          <SummaryTile label="Total roles" value={configSummary.totalRoles} tone="blue" />
          <SummaryTile label="Editable roles" value={configSummary.editableRoles} tone="emerald" />
          <SummaryTile label="System locked" value={configSummary.lockedRoles} tone="amber" />
          <SummaryTile label="Reporting mappings" value={configSummary.reportingMappings} tone="violet" />
        </div>

        <div className="mb-5 flex flex-wrap gap-2 rounded-2xl border border-zinc-200 bg-white p-2 shadow-sm">
          <Button
              className={activeSubTab === "reporting-manager-config" ? "bg-gradient-to-r from-cyan-600 to-blue-600 text-white hover:from-cyan-500 hover:to-blue-500" : ""}
              onClick={() => setActiveSubTab("reporting-manager-config")}
              variant={activeSubTab === "reporting-manager-config" ? "default" : "ghost"}
          >
            <Shield className="mr-2 h-4 w-4" />
            Reporting Manager Config
          </Button>
          <Button
              className={activeSubTab === "role-config" ? "bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white hover:from-violet-500 hover:to-fuchsia-500" : ""}
              onClick={() => setActiveSubTab("role-config")}
              variant={activeSubTab === "role-config" ? "default" : "ghost"}
          >
            <UserCog className="mr-2 h-4 w-4" />
            Role Config
          </Button>
        </div>

        {activeSubTab === "reporting-manager-config" ? (
            <Card className="border-cyan-100 shadow-md shadow-cyan-100/40">
              <CardHeader className="min-h-[132px] bg-gradient-to-r from-cyan-600 to-blue-600 text-white">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-white">
                      <Users className="h-5 w-5" />
                      Reporting Manager Config
                    </CardTitle>
                    <CardDescription className="text-cyan-100">Map each role to the role it reports to.</CardDescription>
                  </div>
                  <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" disabled={saving || availableCreateRoles.length === 0} onClick={() => openCreateModal()} variant="outline">
                    <PlusCircle className="mr-2 h-4 w-4" />
                    Add configuration
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-6">
                <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                  <FixedInputField
                      label="Search role / manager"
                      onChange={(event) => setReportingSearch(event.target.value)}
                      value={reportingSearch}
                      wrapperClassName="sm:max-w-md"
                  />
                  <div className="text-sm text-zinc-500">
                    Showing <span className="font-semibold text-zinc-900">{filteredReportingConfigs.length}</span> of{" "}
                    <span className="font-semibold text-zinc-900">{configs.length}</span>
                  </div>
                </div>
                <div className="overflow-x-auto rounded-2xl border border-zinc-200">
                  <table className="w-full min-w-[900px] text-sm">
                    <thead className="bg-zinc-50/80 text-left">
                    <tr>
                      <th className="px-4 py-3">Role</th>
                      <th className="px-4 py-3">Reports to</th>
                      <th className="px-4 py-3">Status</th>
                      <th className="px-4 py-3 text-right">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {filteredReportingConfigs.map((row) => (
                        <tr className="border-t border-zinc-200" key={row.roleId}>
                          <td className="px-4 py-3 font-medium text-zinc-900">{row.designationRoleName}</td>
                          <td className="px-4 py-3 text-zinc-700">{row.reportsToRoleName ?? "-"}</td>
                          <td className="px-4 py-3">
                            <Badge
                                className={
                                  row.reportsToRoleName
                                      ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                                      : "border-zinc-200 bg-zinc-50 text-zinc-600"
                                }
                            >
                              {rowStatus(row)}
                            </Badge>
                          </td>
                          <td className="px-4 py-3 text-right">
                            {row.id ? (
                                <Button disabled={saving} onClick={() => openEditModal(row)} size="sm" variant="outline">
                                  <PencilLine className="mr-2 h-4 w-4" />
                                  Edit
                                </Button>
                            ) : (
                                <Button disabled={saving} onClick={() => openCreateModal(row.designationRoleName)} size="sm" variant="outline">
                                  <PlusCircle className="mr-2 h-4 w-4" />
                                  Add
                                </Button>
                            )}
                          </td>
                        </tr>
                    ))}
                    </tbody>
                  </table>
                </div>
                {filteredReportingConfigs.length === 0 ? (
                    <div className="rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 px-4 py-6 text-sm text-zinc-600">
                      {configs.length === 0 ? "No reporting manager configuration found." : "No matching reporting manager configuration found."}
                    </div>
                ) : null}
              </CardContent>
            </Card>
        ) : null}

        {activeSubTab === "role-config" ? (
            <Card className="border-violet-100 shadow-md shadow-violet-100/40">
              <CardHeader className="min-h-[132px] bg-gradient-to-r from-violet-600 to-fuchsia-600 text-white">
                <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <CardTitle className="flex items-center gap-2 text-white">
                      <UserCog className="h-5 w-5" />
                      Role Config
                    </CardTitle>
                    <CardDescription className="text-violet-100">Manage role master data and review assignment counts.</CardDescription>
                  </div>
                  <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" disabled={saving} onClick={openCreateRoleModal} variant="outline">
                    <PlusCircle className="mr-2 h-4 w-4" />
                    Add role
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-6">
                <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
                  <FixedInputField
                      label="Search role / description"
                      onChange={(event) => setRoleSearch(event.target.value)}
                      value={roleSearch}
                      wrapperClassName="sm:max-w-md"
                  />
                  <div className="text-sm text-zinc-500">
                    Showing <span className="font-semibold text-zinc-900">{filteredRoleConfigs.length}</span> of{" "}
                    <span className="font-semibold text-zinc-900">{roleConfigs.length}</span>
                  </div>
                </div>
                <div className="overflow-x-auto rounded-2xl border border-zinc-200">
                  <table className="w-full min-w-[980px] text-sm">
                    <thead className="bg-zinc-50/80 text-left">
                    <tr>
                      <th className="px-4 py-3">Role ID</th>
                      <th className="px-4 py-3">Role Name</th>
                      <th className="px-4 py-3">Description</th>
                      <th className="px-4 py-3">Assigned users</th>
                      <th className="px-4 py-3">Rename status</th>
                      <th className="px-4 py-3 text-right">Action</th>
                    </tr>
                    </thead>
                    <tbody>
                    {filteredRoleConfigs.map((role) => (
                        <tr className="border-t border-zinc-200" key={role.id}>
                          <td className="px-4 py-3 text-zinc-700">{role.id}</td>
                          <td className="px-4 py-3 font-medium text-zinc-900">{role.roleName}</td>
                          <td className="px-4 py-3 text-zinc-700">{role.roleDescription || "-"}</td>
                          <td className="px-4 py-3 text-zinc-700">{role.assignedUserCount}</td>
                          <td className="px-4 py-3">
                            <Badge
                                className={
                                  role.roleNameEditable
                                      ? "border-emerald-200 bg-emerald-50 text-emerald-700"
                                      : "border-amber-200 bg-amber-50 text-amber-700"
                                }
                            >
                              {role.roleNameEditable ? "Editable" : "System locked"}
                            </Badge>
                          </td>
                          <td className="px-4 py-3">
                            <div className="flex justify-end gap-2">
                              <Button disabled={saving} onClick={() => void openViewRole(role.id)} size="sm" variant="outline">
                                <Eye className="mr-2 h-4 w-4" />
                                View
                              </Button>
                              <Button disabled={saving} onClick={() => openEditRoleModal(role)} size="sm" variant="outline">
                                <PencilLine className="mr-2 h-4 w-4" />
                                Edit
                              </Button>
                            </div>
                          </td>
                        </tr>
                    ))}
                    </tbody>
                  </table>
                </div>
                {filteredRoleConfigs.length === 0 ? (
                    <div className="mt-4 rounded-2xl border border-dashed border-zinc-200 bg-zinc-50 px-4 py-6 text-sm text-zinc-600">
                      {roleConfigs.length === 0 ? "No roles available." : "No matching roles found."}
                    </div>
                ) : null}
              </CardContent>
            </Card>
        ) : null}

        {modalMode ? (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
              <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-3xl border border-zinc-200 bg-white shadow-2xl shadow-slate-300/40">
                <div className="border-b border-zinc-200 bg-gradient-to-r from-cyan-600 to-blue-600 px-6 py-5 text-white">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-cyan-100">
                        {modalMode === "create" ? "Add configuration" : "Edit configuration"}
                      </p>
                      <h2 className="mt-1 text-xl font-semibold text-white">Reporting Manager Config</h2>
                    </div>
                    <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" onClick={closeModal} variant="outline">
                      Close
                    </Button>
                  </div>
                </div>

                <form onSubmit={handleSubmit}>
                  <div className="grid gap-4 p-6 md:grid-cols-2">
                    {modalMode === "create" ? (
                        <LabeledSelectField
                            label="Role"
                            onChange={(event) => setForm((value) => ({ ...value, designationRoleName: event.target.value }))}
                            value={form.designationRoleName}
                            wrapperClassName="md:col-span-2"
                        >
                          <option value="">Select role</option>
                          {availableCreateRoles.map((role: RoleSummary) => (
                              <option key={role.id} value={role.name}>
                                {role.name}
                              </option>
                          ))}
                        </LabeledSelectField>
                    ) : (
                        <FixedInputField disabled label="Role" value={form.designationRoleName} wrapperClassName="md:col-span-2" />
                    )}

                    <LabeledSelectField
                        label="Reports to role"
                        onChange={(event) => setForm((value) => ({ ...value, reportsToRoleName: event.target.value }))}
                        value={form.reportsToRoleName}
                        wrapperClassName="md:col-span-2"
                    >
                      <option value="">Select reporting manager role</option>
                      {reportToOptions.map((role: RoleSummary) => (
                          <option key={role.id} value={role.name}>
                            {role.name}
                          </option>
                      ))}
                    </LabeledSelectField>
                  </div>

                  <div className="flex items-center justify-end gap-3 border-t border-zinc-200 px-6 py-4">
                    <Button disabled={saving} onClick={closeModal} type="button" variant="outline">
                      Cancel
                    </Button>
                    <Button disabled={saving} type="submit">
                      {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
                      {modalMode === "create" ? "Create" : "Update"}
                    </Button>
                  </div>
                </form>
              </div>
            </div>
        ) : null}

        {roleModalMode ? (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
              <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-3xl border border-zinc-200 bg-white shadow-2xl shadow-slate-300/40">
                <div className="border-b border-zinc-200 bg-gradient-to-r from-violet-600 via-fuchsia-600 to-indigo-600 px-6 py-5 text-white">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.24em] text-violet-100">
                        {roleModalMode === "create" ? "Add role" : "Edit role"}
                      </p>
                      <h2 className="mt-1 text-xl font-semibold text-white">Role Config</h2>
                    </div>
                    <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" onClick={closeRoleModal} variant="outline">
                      Close
                    </Button>
                  </div>
                </div>

                <form onSubmit={handleRoleSubmit}>
                  <div className="grid gap-4 p-6">
                    {roleModalMode === "edit" && editingRole && !editingRole.roleNameEditable ? (
                        <FixedInputField
                            disabled
                            label="Role name"
                            value={roleForm.roleName}
                        />
                    ) : (
                        <FixedInputField
                            label="Role name"
                            onChange={(event) => setRoleForm((value) => ({ ...value, roleName: event.target.value }))}
                            value={roleForm.roleName}
                        />
                    )}
                    <label className="relative block">
                      <span className="pointer-events-none absolute left-3 -top-2 z-10 bg-white px-1 text-xs text-zinc-500">Role description</span>
                      <textarea
                          className="min-h-28 w-full rounded-xl border border-zinc-300 bg-white px-3 pb-2 pt-5 text-sm text-zinc-900 outline-none transition focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
                          onChange={(event) => setRoleForm((value) => ({ ...value, roleDescription: event.target.value }))}
                          value={roleForm.roleDescription}
                      />
                    </label>
                    {roleModalMode === "edit" && editingRole && !editingRole.roleNameEditable ? (
                        <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-700">
                          System role names are locked because they are used by application authorization and workflow rules.
                        </div>
                    ) : null}
                  </div>

                  <div className="flex items-center justify-end gap-3 border-t border-zinc-200 px-6 py-4">
                    <Button disabled={saving} onClick={closeRoleModal} type="button" variant="outline">
                      Cancel
                    </Button>
                    <Button disabled={saving} type="submit">
                      {saving ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : <Save className="mr-2 h-4 w-4" />}
                      {roleModalMode === "create" ? "Create" : "Update"}
                    </Button>
                  </div>
                </form>
              </div>
            </div>
        ) : null}

        {viewRole ? (
            <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/45 p-4 backdrop-blur-sm">
              <div className="max-h-[90vh] w-full max-w-4xl overflow-y-auto rounded-3xl border border-zinc-200 bg-white shadow-2xl shadow-slate-300/40">
                <div className="border-b border-zinc-200 bg-gradient-to-r from-indigo-600 via-violet-600 to-fuchsia-600 px-6 py-5 text-white">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="flex items-center gap-2 text-xl font-semibold">
                        <Eye className="h-5 w-5" />
                        Role Details #{viewRole.id}
                      </h3>
                      <p className="mt-1 text-sm text-indigo-100">{viewRole.roleName}</p>
                    </div>
                    <Button className="border-white/30 bg-white/10 text-white hover:bg-white/20" onClick={() => setViewRole(null)} variant="outline">
                      Close
                    </Button>
                  </div>
                </div>
                <div className="grid gap-3 p-6 md:grid-cols-2">
                  <DetailCard label="Role Name" value={viewRole.roleName} />
                  <DetailCard label="Role ID" value={String(viewRole.id)} />
                  <DetailCard label="Role Description" value={viewRole.roleDescription || "-"} />
                  <DetailCard label="Assigned Users Count" value={String(viewRole.assignedUserCount)} />
                </div>
              </div>
            </div>
        ) : null}
      </>
  );
}

function DetailCard({ label, value }: { label: string; value: string }) {
  return (
      <div className="rounded-2xl bg-zinc-50 px-4 py-3">
        <p className="text-xs font-semibold uppercase tracking-wide text-zinc-500">{label}</p>
        <p className="mt-1 text-sm font-medium text-zinc-900">{value}</p>
      </div>
  );
}

function SummaryTile({
                       label,
                       value,
                       tone,
                     }: {
  label: string;
  value: number;
  tone: "blue" | "emerald" | "amber" | "violet";
}) {
  const toneStyles =
      tone === "blue"
          ? "border-blue-100 bg-gradient-to-br from-blue-50 to-white text-blue-700"
          : tone === "emerald"
              ? "border-emerald-100 bg-gradient-to-br from-emerald-50 to-white text-emerald-700"
              : tone === "amber"
                  ? "border-amber-100 bg-gradient-to-br from-amber-50 to-white text-amber-700"
                  : "border-violet-100 bg-gradient-to-br from-violet-50 to-white text-violet-700";

  return (
      <Card className={`shadow-sm ${toneStyles}`}>
        <CardContent className="p-4">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-zinc-500">{label}</p>
          <p className="mt-2 text-3xl font-semibold text-zinc-900">{value.toLocaleString()}</p>
        </CardContent>
      </Card>
  );
}
