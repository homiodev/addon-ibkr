package org.homio.addon.ibkr;

import jakarta.persistence.Entity;
import org.apache.commons.lang3.StringUtils;
import org.homio.api.Context;
import org.homio.api.ContextVar;
import org.homio.api.entity.BaseEntity;
import org.homio.api.entity.CreateSingleEntity;
import org.homio.api.entity.HasJsonData;
import org.homio.api.entity.HasStatusAndMsg;
import org.homio.api.entity.device.DeviceBaseEntity;
import org.homio.api.entity.types.MiscEntity;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.Icon;
import org.homio.api.model.JSON;
import org.homio.api.model.OptionModel;
import org.homio.api.model.UpdatableValue;
import org.homio.api.service.EntityService;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldGroup;
import org.homio.api.ui.field.UIFieldPort;
import org.homio.api.ui.field.action.HasDynamicUIFields;
import org.homio.api.ui.field.action.v1.UIInputBuilder;
import org.homio.api.ui.field.action.v1.layout.dialog.UIDialogLayoutBuilder;
import org.homio.api.util.CommonUtils;
import org.homio.api.util.SecureString;
import org.homio.api.widget.CustomWidgetDataStore;
import org.homio.api.widget.HasCustomWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

@SuppressWarnings({"JpaAttributeTypeInspection", "JpaAttributeMemberSignatureInspection", "unused"})
@Entity
@CreateSingleEntity
@UISidebarChildren(icon = "fas fa-square-rss", color = "#B33F30", allowCreateItem = false)
public class IbkrEntity extends MiscEntity implements EntityService<IbkrService>,
  HasStatusAndMsg, HasCustomWidget {

  public static final String PROVIDER = "PROVIDER";

  @Override
  protected void assembleMissingMandatoryFields(@NotNull Set<String> fields) {
    if (getUser().isEmpty()) {
      fields.add("user");
    }
    if (getPassword().isEmpty()) {
      fields.add("password");
    }
    if (!context().media().isWebDriverAvailable()) {
      fields.add("webDriver");
    }
  }

  @UIField(order = 1, inlineEditWhenEmpty = true)
  @UIFieldGroup(order = 15, value = "SECURITY", borderColor = "#23ADAB")
  public String getUser() {
    return getJsonData("user", "");
  }

  public void setUser(String value) {
    setJsonData("user", value);
  }

  @UIField(order = 1, disableEdit = true)
  @UIFieldGroup(order = 50, value = "INFO", borderColor = "#9C27B0")
  public String getAccountId() {
    return getJsonData("aid");
  }

  @UIField(order = 2)
  @UIFieldGroup("SECURITY")
  public SecureString getPassword() {
    return getJsonSecure("pwd");
  }

  public void setPassword(String value) {
    setJsonDataSecure("pwd", value);
  }

  @UIField(order = 1, inlineEdit = true)
  @UIFieldGroup("GENERAL")
  public boolean isStart() {
    return getJsonData("start", false);
  }

  public void setStart(boolean start) {
    setJsonData("start", start);
  }

  @UIField(order = 25)
  @UIFieldPort(min = 1025)
  @UIFieldGroup("GENERAL")
  public int getPort() {
    return getJsonData("port", 5000);
  }

  public void setPort(int value) {
    setJsonData("port", value);
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "ibkr";
  }

  @Override
  public @Nullable String getDefaultName() {
    return "IBKR";
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("port");
  }

  @Override
  public @NotNull Class<IbkrService> getEntityServiceItemClass() {
    return IbkrService.class;
  }

  @Override
  public @Nullable IbkrService createService(@NotNull Context context) {
    return new IbkrService(context, this);
  }

  @Override
  public boolean isDisableDelete() {
    return true;
  }

  public String getUrl(String path) {
    return "http://localhost:" + getPort() + "/v1/api/" + path;
  }

  @Override
  public void assembleActions(UIInputBuilder uiInputBuilder) {
    uiInputBuilder
      .addOpenDialogSelectableButton(
        "CREATE_IBKR_WIDGET",
        new Icon("fas fa-table-list", "#91293E"),
        (context, params) -> {
          createIbkrWidget(context, params.getString("tab"));
          getOrCreateService(context()).ifPresent(ServiceInstance::restartService);
          return ActionResponseModel.success();
        })
      .editDialog(
        builder ->
          builder.addFlex(
            "main",
            flex -> flex.addSelectBoxWidgetTab(context())));

    uiInputBuilder.addSelectableButton("VIEW_AS_TABLE", new Icon("fas fa-table"), (context, params) ->
      updateWidgetView(context, params, View.table));

    uiInputBuilder.addSelectableButton("VIEW_AS_BLOCKS", new Icon("fas fa-border-none"), (context, params) ->
      updateWidgetView(context, params, View.block));

    uiInputBuilder
      .addOpenDialogSelectableButton("ADD_IBKR_VARIABLE", new Icon("fas fa-money-bill-1"), (context, params) -> {
        String groupId = params.getString("group");
        createVariables(params, context, groupId);
        return ActionResponseModel.success();
      })
      .editDialog(IbkrEntity::configureDialog);
  }

  private void createVariables(JSONObject params, Context context, String groupId) {
    String ticker = params.getString("ticker");
    TickType tickType = TickType.valueOf(params.getString("type"));
    Icon icon = new Icon(
      params.getString("icon"),
      StringUtils.defaultIfEmpty(params.optString("color"), "#438A45"));

    context
      .var()
      .createVariable(
        groupId,
        String.valueOf(params.getString("type").hashCode()),
        params.getString("name"),
        ContextVar.VariableType.Float,
        builder ->
          builder
            .setUnit(params.getString("unit"))
            .set(PROVIDER, getEntityID())
            .set("type", tickType.name())
            .set("ticker", ticker)
            .setIcon(icon));
  }

  private @NotNull BaseEntity createIbkrWidget(Context context, String tabId) {
    return context
      .widget()
      .createCustomWidget(
        getEntityID(),
        tabId,
        builder ->
          builder
            .code(CommonUtils.readFile("code.js"))
            .css(CommonUtils.readFile("style.css"))
            .parameterEntity(getEntityID()));
  }

  private static void configureDialog(UIDialogLayoutBuilder dialogBuilder) {
    dialogBuilder.addFlex(
      "main",
      flex -> {
        flex.addTextInput("name", "IBKR goog price", true);
        flex.addTextInput("ticker", "GOOG", true);
        flex.addIconPicker("icon", "fas fa-money-bill-trend-up");
        flex.addColorPicker("color", "#438A45");
        flex.addSelectBox("type")
          .setOptions(OptionModel.enumList(TickType.class))
          .setValue(TickType.LAST_PRICE.name());
        flex.addTextInput("unit", "", false);
        flex.addSelectBox("group").setLazyVariableGroup().setRequired(true);
      });
  }

  private ActionResponseModel updateWidgetView(Context context, JSONObject params, View view) {
    String widgetEntityID = params.getString("entityID");
    DeviceBaseEntity widget = context.db().getRequire(widgetEntityID);
    String currentView = widget.getJsonData("view", View.table.name());
    if (!view.name().equals(currentView)) {
      widget.setJsonData("view", view);
      context.db().save(widget);
    }
    return null;
  }

  @Override
  public void assembleUIFields(@NotNull HasDynamicUIFields.UIFieldBuilder uiFieldBuilder, @NotNull HasJsonData sourceEntity) {
    UpdatableValue<String> sort = UpdatableValue.wrap(sourceEntity, Sort.positions.name(), "defaultSort");
    uiFieldBuilder.addSelect(1, sort, OptionModel.enumList(Sort.class));

    UpdatableValue<Boolean> showSummary = UpdatableValue.wrap(sourceEntity, false, "showSummary");
    uiFieldBuilder.addSwitch(2, showSummary);

    UpdatableValue<Boolean> showTrades = UpdatableValue.wrap(sourceEntity, false, "showTrades");
    uiFieldBuilder.addSwitch(3, showTrades);

    UpdatableValue<String> tableBackground = UpdatableValue.wrap(sourceEntity, "#424242", "tableBackground");
    uiFieldBuilder.addColorPicker(4, tableBackground);

    UpdatableValue<String> tableBackgroundOdd = UpdatableValue.wrap(sourceEntity, "#5c5c5c", "tableBackgroundOdd");
    uiFieldBuilder.addColorPicker(5, tableBackgroundOdd);

    UpdatableValue<String> view = UpdatableValue.wrap(sourceEntity, View.table.name(), "view");
    uiFieldBuilder.addSelect(10, view, OptionModel.enumList(View.class));
  }

  @Override
  public void setWidgetDataStore(@NotNull CustomWidgetDataStore customWidgetDataStore, @NotNull String widgetEntityID, @NotNull JSON widgetData) {
    getService().setWidgetDataStore(customWidgetDataStore);
  }

  @Override
  public void removeWidgetDataStore(@NotNull String widgetEntityID) {

  }

  @Override
  public @NotNull BaseEntity createWidget(@NotNull Context context, @NotNull String name, @NotNull String tabId, int width, int height) {
    return this.createIbkrWidget(context, tabId);
  }

  @Override
  public @Nullable Map<String, Icon> getAvailableWidgets() {
    return Map.of("IBKR", new Icon("fas fa-table-list", "#91293E"));
  }

  public enum Sort {
    price, positions
  }

  public enum View {
    table, block
  }
}
