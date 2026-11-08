import type { PageDto } from "@/api/dto";
import type { Page } from "@/types/page";

export function toPage<TDto, T>(
  dto: PageDto<TDto>,
  mapItem: (item: TDto) => T,
): Page<T> {
  return {
    items: dto.content.map(mapItem),
    page: dto.page,
    size: dto.size,
    totalElements: dto.totalElements,
    totalPages: dto.totalPages,
  };
}
