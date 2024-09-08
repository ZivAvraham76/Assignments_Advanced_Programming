import yaml
import errors

from item import Item
from shopping_cart import ShoppingCart

class Store:
    def __init__(self, path):
        with open(path) as inventory:
            items_raw = yaml.load(inventory, Loader=yaml.FullLoader)['items']
        self._items = self._convert_to_item_objects(items_raw)
        self._shopping_cart = ShoppingCart()

    @staticmethod
    def _convert_to_item_objects(items_raw):
        return [Item(item['name'],
                     int(item['price']),
                     item['hashtags'],
                     item['description'])
                for item in items_raw]

    def get_items(self) -> list:
        return self._items

    #Count the common hashtags between the given item and items in the shopping cart.
    def count_hashtags(self,item: Item)-> int:
        count = 0
        tags = [tag for item in self._shopping_cart.cart_items for tag in item.hashtags]
        for hashtag in item.hashtags:
            count += tags.count(hashtag)
        return count

    #Search for items by name, return sorted list
    def search_by_name(self, item_name: str) -> list:
        match_items = [item for item in self._items if item_name in item.name]
        match_items.sort(key=lambda x: x.name)
        sorted_items = sorted(match_items,key=lambda x: self.count_hashtags(x), reverse=True)
        return [item for item in sorted_items if item not in self._shopping_cart.cart_items]

    #Search for items by hashtag, return sorted list
    def search_by_hashtag(self, hashtag: str) -> list:
        match_items = [item for item in self._items if hashtag in item.hashtags]
        match_items.sort(key=lambda x: x.name)
        sorted_items = sorted(match_items, key=lambda x: self.count_hashtags(x), reverse=True)
        return [item for item in sorted_items if item not in self._shopping_cart.cart_items]

    #Add an item to the shopping cart by name
    def add_item(self, item_name: str):
        match_items = [item for item in self._items if item_name in item.name]
        if match_items == [] :
            raise errors.ItemNotExistError
        if len(match_items) > 1:
            raise errors.TooManyMatchesError
        for item in self._shopping_cart.cart_items:
            if item.name == item_name:
                raise errors.ItemAlreadyExistsError
        else:
            self._shopping_cart.add_item(match_items[0])

    #remove an item to the shopping cart by name
    def remove_item(self, item_name: str):
        match_items = [item for item in self._items if item_name in item.name]
        if match_items == []:
            raise errors.ItemNotExistError
        if len(match_items) > 1:
            raise errors.TooManyMatchesError
        else:
            self._shopping_cart.cart_items.remove(match_items[0])

    #Calculate the total price of items in the shopping cart
    def checkout(self) -> int:
        checkout = 0
        for item in self._shopping_cart.cart_items:
            checkout += item.price
        return checkout
