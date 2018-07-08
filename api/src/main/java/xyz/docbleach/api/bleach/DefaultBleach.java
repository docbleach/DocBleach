package xyz.docbleach.api.bleach;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

/**
 * A composite bleach based on all the {@link Bleach} implementations available through the {@link
 * ServiceLoader service provider mechanism}.
 */
public class DefaultBleach extends CompositeBleach {

  public DefaultBleach() {
    super(getDefaultBleaches());
  }

  /**
   * Finds all statically loadable bleaches
   *
   * @return ordered list of statically loadable bleaches
   */
  private static Bleach[] getDefaultBleaches() {
    ServiceLoader<Bleach> services = ServiceLoader.load(Bleach.class);

    Collection<Bleach> list = new ArrayList<>();
    services.forEach(list::add);

    return list.toArray(new Bleach[0]);
  }
}
