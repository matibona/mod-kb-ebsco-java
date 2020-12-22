package org.folio.rest.converter.costperuse.export;

import static org.folio.common.ListUtils.mapItems;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.folio.rest.jaxrs.model.ResourceCostPerUseCollection;
import org.folio.service.locale.LocaleSettings;
import org.folio.service.uc.export.TitleExportModel;

@Component
public class PackageTitlesCostPerUseCollectionToExportConverter {

  @Autowired
  private PackageTitleCostPerUseConverter resourceCostPerUseExportItemConverter;

  private NumberFormat currencyFormatter;

  public List<TitleExportModel> convert(ResourceCostPerUseCollection resourceCostPerUseCollection, String platform, String year, LocaleSettings localeSettings) {
    var data = resourceCostPerUseCollection.getData();
    var currency = resourceCostPerUseCollection.getParameters().getCurrency();
    setLocaleSettings(localeSettings);
    return mapItems(data, item -> resourceCostPerUseExportItemConverter.convert(item, platform, year, currency, currencyFormatter));
  }

  private void setLocaleSettings(LocaleSettings localeSettings) {
    Locale userLocale = Locale.forLanguageTag(localeSettings.getLocale());
    currencyFormatter = NumberFormat.getCurrencyInstance(userLocale);
    currencyFormatter.setRoundingMode(RoundingMode.HALF_UP);
    currencyFormatter.setMaximumFractionDigits(2);
    DecimalFormatSymbols decimalFormatSymbols = ((DecimalFormat) currencyFormatter).getDecimalFormatSymbols();
    decimalFormatSymbols.setCurrencySymbol("");
    ((DecimalFormat) currencyFormatter).setDecimalFormatSymbols(decimalFormatSymbols);
  }
}
